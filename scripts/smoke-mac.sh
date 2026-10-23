#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/lib/process.sh
source scripts/lib/build-lock.sh
[[ "$(uname -s)" == Darwin ]] || { echo "Run on the remote Mac." >&2; exit 1; }
[[ "${KAURI_ISOLATED_DISPLAY:-}" == 1 ]] || {
    echo "GUI smoke opens ordinary windows. Set KAURI_ISOLATED_DISPLAY=1 only on an owned graphical runner, not the user's desktop." >&2
    exit 1
}
binary="${1:-build/release/macosArm64/Kauri}"
[[ -x "$binary" ]] || { echo "Missing executable: $binary" >&2; exit 1; }
results="${KAURI_SMOKE_RESULTS:-$(pwd)/build/smoke}"
directory=$(mktemp -d "${TMPDIR:-/tmp/}kauri-standalone.XXXXXX")
pid=""
cleanup() {
    terminate_child "$pid" 2>/dev/null
    rm -rf "$directory"
    release_build_lock
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
acquire_build_lock
mkdir -p "$results"
results="$(cd "$results" && pwd)"

# Copy only the executable: no resource folder, working directory, or build tree.
cp "$binary" "$directory/Kauri"
binary="$directory/Kauri"
(
    cd "$directory"
    # Ordinary visible window; only run in an owned graphical session.
    exec "$binary" --startup-check
) >"$results/startup.log" 2>&1 &
pid=$!
for ((i=0; i<45; i++)); do
    if ! kill -0 "$pid" 2>/dev/null; then break; fi
    sleep 1
done
if kill -0 "$pid" 2>/dev/null; then
    cat "$results/startup.log"
    echo "Ordinary startup/close exceeded 45 seconds" >&2
    exit 1
fi
startup_result=0
wait "$pid" || startup_result=$?
pid=""
cat "$results/startup.log"
[[ "$startup_result" == 0 ]] || { echo "Ordinary startup exited $startup_result" >&2; exit "$startup_result"; }
grep '^KAURI_STARTUP PASS:' "$results/startup.log"
if grep -q KAURI_SMOKE "$results/startup.log"; then
    echo "Ordinary startup unexpectedly ran smoke" >&2
    exit 1
fi
rm -f "$results/result.txt" "$results/window.png" "$results/window.tiff"
(
    cd "$directory"
    exec "$binary" --smoke --result-dir "$results"
) >"$results/process.log" 2>&1 &
pid=$!
for ((i=0; i<150; i++)); do
    if ! kill -0 "$pid" 2>/dev/null; then break; fi
    sleep 1
done
if kill -0 "$pid" 2>/dev/null; then
    cat "$results/process.log"
    echo "GUI smoke exceeded 150 seconds" >&2
    exit 1
fi
result=0
wait "$pid" || result=$?
pid=""
cat "$results/process.log"
[[ "$result" == 0 ]] || { echo "GUI smoke exited $result" >&2; exit "$result"; }
grep '^PASS:' "$results/result.txt"
grep '^KAURI_CLEANUP PASS:' "$results/process.log"
grep '^KAURI_POLICY blocked navigation$' "$results/process.log"
grep '^KAURI_POLICY blocked popup$' "$results/process.log"
if [[ -f "$results/window.tiff" ]]; then
    sips -s format png "$results/window.tiff" --out "$results/window.png" >/dev/null
    rm "$results/window.tiff"
fi
[[ -s "$results/window.png" ]] || { echo "Missing UI capture." >&2; exit 1; }
