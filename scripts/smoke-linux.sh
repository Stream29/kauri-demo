#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ "$(uname -s)" == Linux ]] || { echo "Run this smoke test on Linux." >&2; exit 1; }
[[ "${KAURI_ISOLATED_DISPLAY:-}" == 1 ]] || {
    echo "Set KAURI_ISOLATED_DISPLAY=1 only for an owned isolated display, not the user's desktop." >&2
    exit 1
}
[[ -n "${WAYLAND_DISPLAY:-${DISPLAY:-}}" ]] || {
    echo "Provide an isolated headless display, not the user's desktop." >&2
    exit 1
}
case "$(uname -m)" in
    x86_64) target=linuxX64 ;;
    aarch64) target=linuxArm64 ;;
    *) echo "Unsupported Linux architecture" >&2; exit 1 ;;
esac
binary="${1:-build/release/$target/Kauri}"
mkdir -p "build/smoke-$target"
results="$(pwd)/build/smoke-$target"
directory=$(mktemp -d)
trap 'rm -rf "$directory"' EXIT
cp "$binary" "$directory/Kauri"
rm -f "$results/result.txt"
(
    cd "$directory"
    timeout --kill-after=5s 150s ./Kauri --smoke --result-dir "$results"
) >"$results/process.log" 2>&1 || result=$?
cat "$results/process.log"
[[ "${result:-0}" == 0 ]] || exit "$result"
grep '^PASS:' "$results/result.txt"
