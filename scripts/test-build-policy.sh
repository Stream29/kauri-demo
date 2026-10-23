#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "$(dirname "$0")/.."
[[ "$(uname -s)" == Darwin ]] || { echo "Run policy checks on the Mac builder." >&2; exit 1; }
source scripts/lib/build-lock.sh
source scripts/lib/process.sh
acquire_build_lock
fixture=""
child=""
cleanup_policy() {
    terminate_child "$child"
    if [[ -n "$fixture" ]]; then rm -rf "$fixture"; fi
    release_build_lock
}
trap cleanup_policy EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
fixture=$(mktemp -d "$(pwd)/.gradle/kauri/policy-test.XXXXXX")

expect_failure() {
    local expected="$1" label="$2" result=0
    shift 2
    "$@" >"$fixture/$label.log" 2>&1 || result=$?
    [[ "$result" == "$expected" ]] || {
        cat "$fixture/$label.log"
        echo "FAIL: $label returned $result, expected $expected" >&2
        exit 1
    }
    echo "PASS: $label"
}
expect_failure 2 worker-limit bash scripts/gradle-mac.sh :help --max-workers=8
expect_failure 2 jvm-override bash scripts/gradle-mac.sh :help -Dorg.gradle.jvmargs=-Xmx8g
expect_failure 2 ambiguous-selector bash scripts/gradle-mac.sh check
expect_failure 2 client-diagnostic bash scripts/gradle-mac.sh --version
expect_failure 2 project-routing bash scripts/gradle-mac.sh :help --project-dir ..
expect_failure 2 external-init bash scripts/gradle-mac.sh :help --init-script missing.gradle.kts
expect_failure 1 concurrent-invocation env KAURI_BUILD_LOCK_PID=0 bash scripts/gradle-mac.sh :help
grep 'Repository build lock' "$fixture/concurrent-invocation.log" >/dev/null
for kind in stale unrelated; do
    mkdir -p "$fixture/$kind"
    owner=2147483647
    if [[ "$kind" == unrelated ]]; then owner=$PPID; fi
    expect_failure 1 "$kind-lock" bash -c '
        cd "$1"
        mkdir -p .gradle/kauri/gradle-mac.lock
        printf "%s\n" "$2" >.gradle/kauri/gradle-mac.lock/owner.pid
        source "$3"
        acquire_build_lock
    ' -- "$fixture/$kind" "$owner" "$(pwd)/scripts/lib/build-lock.sh"
done
grep 'manually removing' "$fixture/stale-lock.log" >/dev/null

cat >"$fixture/failing-app" <<'APP'
#!/bin/sh
echo EXPECTED_SMOKE_FAILURE
exit 7
APP
chmod +x "$fixture/failing-app"
expect_failure 1 smoke-needs-runner env KAURI_ISOLATED_DISPLAY=0 \
    bash scripts/smoke-mac.sh "$fixture/failing-app"
grep 'owned graphical runner' "$fixture/smoke-needs-runner.log" >/dev/null
if grep -q EXPECTED_SMOKE_FAILURE "$fixture/smoke-needs-runner.log"; then
    echo "GUI guard started the application." >&2
    exit 1
fi
expect_failure 7 smoke-failure-diagnostics env KAURI_ISOLATED_DISPLAY=1 KAURI_SMOKE_RESULTS="$fixture/smoke" \
    bash scripts/smoke-mac.sh "$fixture/failing-app"
grep EXPECTED_SMOKE_FAILURE "$fixture/smoke-failure-diagnostics.log" >/dev/null
[[ "$(cat "$KAURI_BUILD_LOCK_PATH/owner.pid")" == "$$" ]] || {
    echo "Nested helper released its parent's lock." >&2
    exit 1
}

bash -c 'trap "" TERM; exec sleep 30' &
child=$!
sleep 0.2
started=$SECONDS
terminate_child "$child"
if kill -0 "$child" 2>/dev/null; then echo "Child cleanup failed." >&2; exit 1; fi
child=""
[[ $((SECONDS - started)) -le 8 ]] || { echo "Child cleanup was not bounded." >&2; exit 1; }
echo "PASS: bounded-child-cleanup"
