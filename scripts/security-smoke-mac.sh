#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "$(dirname "$0")/.."
source scripts/lib/process.sh
source scripts/lib/build-lock.sh
[[ "$(uname -s)" == Darwin ]] || { echo "Run on the remote Mac." >&2; exit 1; }
binary="${1:-build/release/macosArm64/Kauri}"
directory=$(mktemp -d)
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
"$binary" --serve --url-file "$directory/endpoint" >"$directory/server.log" 2>&1 &
pid=$!
for ((i=0; i<100; i++)); do
    [[ -s "$directory/endpoint" ]] && break
    if ! kill -0 "$pid" 2>/dev/null; then cat "$directory/server.log"; exit 1; fi
    sleep 0.1
done
[[ -s "$directory/endpoint" ]] || { cat "$directory/server.log"; exit 1; }
endpoint=$(cat "$directory/endpoint")
origin="${endpoint%%/#*}"
token="${endpoint##*#}"
check_status() {
    expected="$1"
    label="$2"
    shift 2
    if ! actual=$(curl --silent --show-error --max-time 5 --path-as-is -o /dev/null -w '%{http_code}' "$@"); then
        echo "FAIL: $label request did not complete" >&2
        return 1
    fi
    [[ "$actual" == "$expected" ]] || { echo "FAIL: $label returned $actual, expected $expected" >&2; exit 1; }
    echo "PASS: $label ($actual)"
}
check_status 200 "native health" "$origin/health"
check_status 403 "foreign Origin" -H 'Origin: https://example.invalid' "$origin/rpc/$token"
check_status 403 "foreign Host" -H 'Host: example.invalid' "$origin/health"
check_status 403 "missing WebSocket Origin" "$origin/rpc/$token"
check_status 403 "invalid capability" -H "Origin: $origin" "$origin/rpc/invalid"
check_status 404 "encoded path traversal" "$origin/%2e%2e/note.txt"
check_status 404 "raw path traversal" "$origin/../note.txt"

frontend="app/frontend/build/dist/wasmJs/productionExecutable"
asset=""
largest=0
while IFS= read -r candidate; do
    size=$(stat -f%z "$candidate")
    if ((size > largest)); then asset="$candidate"; largest="$size"; fi
done < <(find "$frontend" -type f -name '*.wasm')
[[ -n "$asset" ]] || { echo "Missing streaming test fixture." >&2; exit 1; }
resource="${asset#"$frontend"/}"
result=0
curl --silent --show-error --limit-rate 16k --max-time 0.5 \
    -o "$directory/partial" "$origin/$resource" 2>"$directory/slow-client.log" || result=$?
[[ "$result" == 28 && -s "$directory/partial" ]] || {
    echo "Slow-client fixture did not interrupt an active transfer (curl $result)." >&2
    exit 1
}
[[ $(stat -f%z "$directory/partial") -lt "$largest" ]]
echo "PASS: interrupted slow resource consumer"
curl --silent --show-error --fail --max-time 30 -D "$directory/headers" \
    -o "$directory/resource" "$origin/$resource"
grep -i '^transfer-encoding: chunked' "$directory/headers" >/dev/null
if grep -i '^content-length:' "$directory/headers" >/dev/null; then
    echo "Unexpected Content-Length in the streaming response." >&2
    exit 1
fi
cmp "$asset" "$directory/resource"
echo "PASS: chunked resource bytes match ($largest bytes), fresh stream after cancellation"
