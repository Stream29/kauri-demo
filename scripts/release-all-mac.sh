#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ "$(uname -s)" == Darwin ]] || { echo "Build on the remote Mac." >&2; exit 1; }
[[ "$#" -le 1 ]] || { echo "Expected only an optional all or macosArm64 scope." >&2; exit 2; }
scope="${1:-all}"
case "$scope" in
    all)
        targets=(macosArm64 mingwX64 linuxX64 linuxArm64)
        tasks=(
            :app:desktop:linkReleaseExecutableMacosArm64
            :app:desktop:linkReleaseExecutableMingwX64
            :app:desktop:linkReleaseExecutableLinuxX64
            :app:desktop:linkReleaseExecutableLinuxArm64
        )
        ;;
    macosArm64)
        targets=(macosArm64)
        tasks=(:app:desktop:linkReleaseExecutableMacosArm64)
        ;;
    *) echo "Expected all or macosArm64." >&2; exit 2 ;;
esac
source scripts/lib/build-lock.sh
acquire_build_lock
validation_temporary="build/release/VALIDATION.txt.$$.tmp"
staged_temporary=""
cleanup_release() {
    rm -f "$validation_temporary"
    if [[ -n "$staged_temporary" ]]; then rm -f "$staged_temporary"; fi
    release_build_lock
}
trap cleanup_release EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

mkdir -p build/release
rm -f build/release/VALIDATION.txt build/release/SHA256SUMS
bash scripts/gradle-mac.sh "${tasks[@]}" :app:shared:macosArm64Test \
    :app:desktop:macosArm64Test :framework:macosArm64Test \
    --configuration-cache --configuration-cache-problems=fail
node_version=$(tr -d '\r\n' <.nvmrc)
"${NVM_DIR:-$HOME/.nvm}/versions/node/v$node_version/bin/node" --test scripts/test-embedding.mjs
for target in "${targets[@]}"; do
    extension=kexe
    name=Kauri
    if [[ "$target" == mingwX64 ]]; then extension=exe; name=Kauri.exe; fi
    directory="build/release/$target"
    mkdir -p "$directory"
    staged_temporary="$directory/$name.$$.tmp"
    cp "app/desktop/build/bin/$target/releaseExecutable/Kauri.$extension" "$staged_temporary"
    if [[ "$target" == macosArm64 ]]; then
        xcrun strip -x "$staged_temporary"
        codesign --force --sign - --identifier dev.kauri.Kauri "$staged_temporary"
        codesign --verify --strict "$staged_temporary"
    fi
    mv "$staged_temporary" "$directory/$name"
    staged_temporary=""
    (cd "$directory" && shasum -a 256 "$name" >SHA256SUMS)
done
if [[ "$scope" == all ]]; then
    (cd build/release && shasum -a 256 macosArm64/Kauri mingwX64/Kauri.exe \
        linuxX64/Kauri linuxArm64/Kauri >SHA256SUMS)
fi
gui="NOT RUN (no owned graphical runner selected)"
if [[ "${KAURI_ISOLATED_DISPLAY:-}" == 1 ]]; then
    bash scripts/smoke-mac.sh build/release/macosArm64/Kauri
    gui="PASS (ordinary startup/close, standalone Compose/kRPC/Flow, native navigation/popup guards)"
else
    echo "NOT RUN: native GUI smoke requires an owned graphical runner."
fi
bash scripts/security-smoke-mac.sh build/release/macosArm64/Kauri
build/release/macosArm64/Kauri --licenses >/dev/null

if [[ "$scope" == all ]]; then
    (cd build/release && shasum -a 256 -c SHA256SUMS)
    manifest=build/release/SHA256SUMS
else
    (cd build/release/macosArm64 && shasum -a 256 -c SHA256SUMS)
    manifest=build/release/macosArm64/SHA256SUMS
fi
{
    printf 'Scope: %s\n' "$scope"
    printf 'macosArm64: non-GUI Native/resource tests, licenses CLI and HTTP security/streaming checks passed.\n'
    printf 'macosArm64 GUI: %s. Browser-context isolation/cleanup and Cocoa failure-callback cleanup are not validated by this script.\n' "$gui"
    if [[ "$scope" == all ]]; then
        printf 'mingwX64, linuxX64, linuxArm64: built, not GUI-runtime validated by this script.\n'
    fi
    cat "$manifest"
} >"$validation_temporary"
mv "$validation_temporary" build/release/VALIDATION.txt
echo "Staged release artifacts with explicit validation coverage: build/release/"
