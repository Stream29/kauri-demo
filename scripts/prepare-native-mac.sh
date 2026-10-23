#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ "$(uname -s)" == Darwin ]] || { echo "Prepare native dependencies on the Mac." >&2; exit 1; }
source scripts/lib/build-lock.sh
acquire_build_lock
trap release_build_lock EXIT
scope="${1:-all}"
[[ "$scope" == all || "$scope" == macosArm64 ]] || { echo "Expected all or macosArm64"; exit 2; }
property() { sed -n "s/^$1=//p" gradle/native-toolchains.properties; }
version=$(sed -n 's/^kotlin = "\(.*\)"/\1/p' gradle/libs.versions.toml)
[[ "$version" == "$(property kotlinNativeVersion)" ]] || {
    echo "Update the native preparation description together with Kotlin." >&2; exit 1;
}
konan_home="${KONAN_DATA_DIR:-$HOME/.konan}"
native="$konan_home/kotlin-native-prebuilt-macos-aarch64-$version"
if [[ ! -x "$native/bin/konanc" ]]; then
    bash scripts/gradle-mac.sh :app:shared:downloadKotlinNativeDistribution
fi
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
targets=(macos_arm64)
if [[ "$scope" == all ]]; then targets+=(mingw_x64 linux_x64 linux_arm64); fi
for target in "${targets[@]}"; do
    nice -n 10 "$native/bin/konanc" -J-Xmx256m -target "$target" -Xcheck-dependencies
done
konan="$konan_home/dependencies"
llvm="$konan/$(property llvm)"
[[ -x "$llvm/bin/clang++" ]] || {
    echo "Kotlin/Native dependency installation did not provide the pinned LLVM." >&2; exit 1;
}
root="$PWD"
deps="$root/build/native-deps"
cache="${KAURI_NATIVE_CACHE:-$HOME/Library/Caches/Kauri/native}"
mkdir -p "$deps" "$cache"
ln -sf "$llvm/bin/clang" "$deps/clang"

fetch() {
    local url="$1" file="$2" hash="$3"
    if [[ ! -f "$file" ]]; then
        curl -fsSL --retry 2 --connect-timeout 15 --max-time 180 "$url" -o "$file.tmp"
        mv "$file.tmp" "$file"
    fi
    printf '%s  %s\n' "$hash" "$file" | shasum -a 256 -c -
}
if [[ "$scope" == macosArm64 ]]; then
    echo "Kotlin/Native toolchain ready; direct macOS host uses system AppKit/WebKit."
    exit 0
fi

for arch in amd64 arm64; do
    if [[ "$arch" == amd64 ]]; then
        target=linuxX64; triple=x86_64-unknown-linux-gnu; multiarch=x86_64-linux-gnu
    else
        target=linuxArm64; triple=aarch64-unknown-linux-gnu; multiarch=aarch64-linux-gnu
    fi
    sdk="$deps/sdk-$arch"
    mkdir -p "$sdk"
    while read -r hash path; do
        [[ "$path" == *"_$arch.deb" || "$path" == *_all.deb ]] || continue
        archive="$cache/$(basename "$path")"
        fetch "https://deb.debian.org/debian/$path" "$archive" "$hash"
        member=$(/usr/bin/ar t "$archive" | awk '/^data[.]tar/ {print;exit}')
        /usr/bin/ar p "$archive" "$member" | /usr/bin/tar -xf - -C "$sdk" --exclude='*/share/*'
    done <gradle/linux-sdk.lock
    while IFS= read -r -d '' link; do
        destination=$(readlink "$link")
        if [[ "$destination" == /usr/* || "$destination" == /lib/* ]]; then
            ln -sf "$sdk$destination" "$link"
        fi
    done < <(find "$sdk" -type l -print0)
    mkdir -p "$deps/$target/lib"
    sdk="$deps/sdk-$arch"
    mkdir -p "$deps/$target/lib"
    for library in webkit2gtk-4.1 javascriptcoregtk-4.1 gtk-3 gdk-3 gobject-2.0 glib-2.0 gio-2.0; do
        file="$sdk/usr/lib/$multiarch/lib$library.so"
        [[ -f "$file" ]] || { echo "Missing SDK link input: $file" >&2; exit 1; }
        ln -sf "$file" "$deps/$target/lib/"
    done
done
echo "Native SDK link inputs ready. Ordinary Gradle builds do not rerun this preparation."
