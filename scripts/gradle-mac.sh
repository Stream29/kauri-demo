#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ "$(uname -s)" == Darwin ]] || { echo "Build on the remote Mac, not this machine." >&2; exit 1; }

for argument in "$@"; do
    case "$argument" in
        check|build|test|assemble|clean)
            echo "Use a qualified task path (for example :framework:macosArm64Test); unqualified selectors may run every subproject." >&2
            exit 2
            ;;
        -Dorg.gradle.java.home*|-Dorg.gradle.jvmargs*|-Dorg.gradle.workers.max*|\
        -Dorg.gradle.daemon*|-Dorg.gradle.internal.instrumentation.agent*|\
        --max-workers*|--no-daemon|--stop|--foreground|--gradle-user-home*|-g|-g?*|\
        --init-script*|-I|-I?*|--project-dir*|-p|-p?*|--settings-file*|-c|-c?*|\
        --build-file*|-b|-b?*|--include-build*|--version|-v|-version|--help|-h|--status)
            echo "This helper owns daemon/JVM/worker selection; rejected: $argument" >&2
            exit 2
            ;;
    esac
done

node_version=$(tr -d '\r\n' <.nvmrc)
node_bin="${NVM_DIR:-$HOME/.nvm}/versions/node/v$node_version/bin"
if [[ -x "$node_bin/node" ]]; then export PATH="$node_bin:$PATH"; fi
[[ "$(node --version 2>/dev/null || true)" == "v$node_version" ]] || {
    echo "Install/select Node $node_version with nvm first." >&2
    exit 1
}

state="$(pwd)/.gradle/kauri"
mkdir -p "$state"
source scripts/lib/build-lock.sh
acquire_build_lock
bootstrap=""
cleanup() {
    if [[ -n "$bootstrap" ]]; then rm -rf "$bootstrap"; fi
    release_build_lock
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

version=$(sed -nE 's/.*gradle-([0-9.]+)-(bin|all)\.zip.*/\1/p' gradle/wrapper/gradle-wrapper.properties)
[[ -n "$version" ]] || { echo "Cannot read the pinned Gradle wrapper version." >&2; exit 1; }
registry_root="${GRADLE_USER_HOME:-$HOME/.gradle}/daemon"
[[ "$registry_root" == /* ]] || { echo "GRADLE_USER_HOME must be absolute." >&2; exit 1; }
registry="$registry_root/$version"
current_uid=$(id -u)
daemon_processes() { pgrep -u "$current_uid" -f 'org.gradle.launcher.daemon.bootstrap.GradleDaemon' || true; }
matching_daemons() {
    local candidate command_line
    for candidate in $(daemon_processes); do
        command_line=$(ps -p "$candidate" -o command= 2>/dev/null || true)
        if [[ "$command_line" == *"GradleDaemon $version" && -f "$registry/daemon-$candidate.out.log" ]]; then
            printf '%s\n' "$candidate"
        fi
    done
}
context_field() {
    local context="$1" field="$2" remainder
    [[ "$context" == *"$field="* ]] || return 1
    remainder="${context#*"$field="}"
    printf '%s' "${remainder%%,*}"
}

candidates=$(matching_daemons)
if [[ -z "$candidates" ]]; then
    [[ -z "$(daemon_processes)" ]] || {
        echo "No compatible daemon in this GRADLE_USER_HOME; refusing to compete with existing daemons." >&2
        exit 1
    }
    export JAVA_HOME="${KAURI_JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
    [[ -x "$JAVA_HOME/bin/java" ]] || { echo "Invalid bootstrap JDK." >&2; exit 1; }
    bootstrap=$(mktemp -d "$state/daemon-bootstrap.XXXXXX")
    printf 'rootProject.name = "kauri-daemon-bootstrap"\n' >"$bootstrap/settings.gradle.kts"
    bootstrap_args=$(sed -n 's/^org.gradle.jvmargs=//p' gradle.properties)
    [[ -n "$bootstrap_args" ]] || { echo "Missing project JVM memory policy." >&2; exit 1; }
    echo "No daemon exists; initializing Gradle $version with explicit JDK $JAVA_HOME"
    bootstrap_result=0
    ./gradlew "-Dorg.gradle.java.home=$JAVA_HOME" "-Dorg.gradle.jvmargs=$bootstrap_args" \
        --max-workers=2 --console=plain --info -p "$bootstrap" help \
        >"$state/daemon-bootstrap.log" 2>&1 || bootstrap_result=$?
    owned_pid=$(sed -nE 's/.*daemon \(pid: ([0-9]+)\).*/\1/p' "$state/daemon-bootstrap.log" | head -1)
    if [[ -n "$owned_pid" ]]; then printf '%s\n' "$owned_pid" >"$state/owned-daemon.pid"; fi
    if [[ "$bootstrap_result" != 0 ]]; then cat "$state/daemon-bootstrap.log"; exit "$bootstrap_result"; fi
    rm -rf "$bootstrap"
    bootstrap=""
    candidates=$(matching_daemons)
fi

pid=""
context=""
for candidate in $candidates; do
    candidate_context=$(grep 'DefaultDaemonContext.*javaHome=' "$registry/daemon-$candidate.out.log" | tail -1)
    candidate_home=$(context_field "$candidate_context" javaHome)
    candidate_registry=$(context_field "$candidate_context" daemonRegistryDir)
    [[ "$candidate_registry" == "$registry_root" && -x "$candidate_home/bin/java" ]] || continue
    candidate_status=$(JAVA_HOME="$candidate_home" ./gradlew "-Dorg.gradle.java.home=$candidate_home" --console=plain --status)
    if printf '%s\n' "$candidate_status" | awk -v pid="$candidate" \
        '$1 == pid && $2 == "IDLE" { found = 1 } END { exit !found }'; then
        pid=$candidate
        context=$candidate_context
        export JAVA_HOME="$candidate_home"
        break
    fi
done
[[ -n "$pid" ]] || { echo "No idle compatible daemon; refusing to create a competing JVM." >&2; exit 1; }

options="${context#*daemonOpts=}"
[[ "$options" != "$context" && "$options" == *"]"* ]] || {
    echo "Cannot read daemon JVM options; refusing to guess." >&2
    exit 1
}
options="${options%%]*}"
IFS=',' read -r -a daemon_options <<<"$options"
jvm_args=""
for option in "${daemon_options[@]}"; do
    [[ "$option" == -* ]] || { echo "Ambiguous daemon JVM option; refusing to guess." >&2; exit 1; }
    escaped="${option//\\/\\\\}"
    escaped="${escaped//\"/\\\"}"
    jvm_args="$jvm_args \"$escaped\""
done

export KAURI_EXPECTED_DAEMON_PID="$pid"
echo "Reusing Gradle $version daemon $pid with $JAVA_HOME"
result=0
./gradlew "-Dorg.gradle.java.home=$JAVA_HOME" "-Dorg.gradle.jvmargs=$jvm_args" \
    --max-workers=2 --console=plain --info \
    --init-script "$(pwd)/scripts/verify-daemon.init.gradle.kts" "$@" 2>&1 | tee "$state/gradle-last.log" || result=$?
grep -F "KAURI_DAEMON_OBSERVED $pid" "$state/gradle-last.log" >/dev/null || {
    echo "The build did not pass the daemon identity guard." >&2
    exit 1
}
exit "$result"
