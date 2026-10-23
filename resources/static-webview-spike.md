# Unified static webview: isolated probe

Date: 2026-10-21. Tested webview 0.12.0 at 3ab4b5d722438fc8a13e6ca830c5e2372d19a01d without replacing production hosts or deciding migration.

Historical isolated evidence. A later authorized migration is recorded in [minimal-static-host](minimal-static-host.md); probe binaries are not current releases.

## Conclusion

- **Static webview libraries and Kotlin/Native C API consumers compiled/linked on all four targets.**
- **Mac completed real single-file application integration**: the same executable held Native backend, embedded Compose Wasm, typed kRPC and static webview, not an external server/browser pairing.
- The wrapper could own basic windows/WebViews, but its C API did not cover all security/test configuration; the Mac probe still needed platform code.
- A prebuilt .a does not remove cross-link costs: Linux consumers still need target GTK/WebKitGTK SDK inputs, not just a header/archive.
- This was feasibility, not completed migration/release or a faster/more-reliable comparison.
- Later phase-one scope put routing/dialogs in Compose; profile/offscreen creation limits are in [minimal host policy](minimal-host-policy.md).

## Coverage

| Target | Static archive bytes | Kotlin consumer | Runtime coverage |
| --- | ---: | --- | --- |
| macosArm64 | 132,352 | Basic consumer and independent Kauri app linked | Version query; full offscreen Wasm/kRPC/Canvas; navigation/popup/backend safety |
| mingwX64 | 286,234 | PE x64 console probe linked | No Windows execution, not a Windows GUI Kauri pass |
| linuxX64 | 150,124 | ELF x64 probe linked | Version query with DISPLAY/WAYLAND_DISPLAY unset and LD_BIND_NOW=1 passed; no GTK window |
| linuxArm64 | 149,224 | ELF aarch64 probe linked | Not executed |

- PE imports only system DLLs, not WebView2Loader/libstdc++/libgcc companions. Runtime discovery/environment creation remained untested.
- Linux directly required GTK/WebKitGTK/GLib shared libraries; only the wrapper was static. Unlike the old dlopen host, those libraries became pre-main requirements.
- Mac app probe after strip/ad-hoc signing: **17,030,656 bytes (~16.24 MiB)**, excluding system libraries/frameworks. Different entries/test settings do not form a strict incremental size comparison.

## Mac integration

- Reused compiled app/shared, app/desktop, framework klibs and frontend.o; only an isolated entry and static wrapper link were new, not rewritten business code.
- Ran one copied executable in an empty temporary directory; assets came from it and notes used a new temp directory.
- Real Canvas: 110 sampled colors. Native information, Unicode round-trip, oversize rejection, Flow 1/2/3 and unauthorized RPC 403 passed.
- Native navigation/popup callbacks each logged a rejection during --smoke.
- Separate --serve run opened no window and passed the existing seven-case health/Origin/Host/WebSocket-Origin/capability/traversal script. Those filters belong to the backend, not webview.
- Initial policy target used browser-reserved port 9 and timed out without a callback. Replacing it with localhost on the current server port (foreign to 127.0.0.1 origin) passed; this was a probe issue, not a library failure.
- Caller-owned windows were offscreen from creation and passed to webview_create, never displayed then hidden/activated.
- The wrapper-created visible top-level path and normal foreground open/close were not tested. Coverage was the caller-owned WebView/event loop.
- webview_get_native_handle supplied WKWebView for navigation/UIDelegate, popup/background policy and result capture: wrapper plus platform policy, not zero platform code.

## Build costs

- C++, cinterop and Kotlin builds ran serially/low priority on Mac. One Gradle invocation inspected existing dependencies via daemon-reusing helper; no workstation Gradle/Linux compilation.
- Mac used Xcode/system frameworks. Windows reused Konan LLVM/MinGW and verified SDK headers with upstream MinGW compatibility/built-in loader.
- Official Debian bookworm indexes supplied 37 packages per Linux architecture with per-package SHA-256 verification, yielding ~164/165 MiB temporary SDKs; no system packages were installed or embedded.
- Wrapper compilation used SDK headers/GCC 8.3 sysroot. Kotlin final links needed real SDK GTK/WebKitGTK .so inputs; GNU libstdc++ was statically included.
- --allow-shlib-undefined deferred shared-library transitive resolution to targets. x64 version/immediate-binding checks did not prove GTK windows/later modules/other targets; arm64 was link only.
- Mac-based application builds still required target link inputs beyond precompiled archives. No artifact distribution service was built.

## Remaining switch-over work

- Security: C API lacked complete navigation/popup/profile settings. Mac hooks were tested; Windows/Linux equivalents and callback/exit lifetime still needed target runs.
- Defaults: pinned Cocoa enabled clipboard/paste preferences and file panels, unused by the frontend. Probe success did not prove permission equivalence.
- Replacing UIDelegate replaced upstream file-panel handling. Forward/reject behavior and changing delegate responsibilities needed explicit decisions.
- Windows built-in loader still used an internal entry and attempted a loader DLL. Preserve search hardening; absent static imports do not mean no dynamic load attempt.
- Direct Linux GUI dependencies changed missing-library diagnostics and potentially --licenses/headless behavior.
- Mac testing needed no upstream source edits but still required native-handle customization; it did not validate every platform's profile/permission/offscreen behavior.

## Evidence, cleanup and review

- Local/Mac build/static-webview-spike contained four archives, consumers, Mac app probe, checksums, header/license and screenshot.
- StaticKauri supported only --smoke/--serve, not the ordinary production entry. Basic consumers only queried version; no visible-window option ran.
- build/review/static-webview-* held sources, build/SDK-index/dependency/runtime evidence. Raw capability files/temporary notes were not retained.
- Temporary probes/SDKs and project locks were removed; only the task-created verified-idle daemon was stopped. Production release hashes still verified; no commit in that iteration.
- agy Gemini 3.8 Flash read-only review prompted clearer smoke/serve separation, SDK requirements on both compilation/consumption sides and delegate effects.
- Rejected an unsupported claim that UIDelegate replacement necessarily broke JS dispatch/title/window lifetime; pinned source assigned the replaced file-panel responsibility specifically.
- Raw review: ignored build/review/agy-static-webview-spike.json, not target execution.

## Sources

- [Static targets](https://github.com/webview/webview/blob/0.12.0/core/CMakeLists.txt), [build dependencies/options](https://github.com/webview/webview/blob/0.12.0/README.md): isolated commands compiled the same core/src/webview.cc without changing the reference submodule.
- [Pinned C API/implementation](https://github.com/webview/webview/blob/0.12.0/core/include/webview/webview.h): caller-owned windows/native handles and configuration limits.
- Debian [WebKitGTK development](https://packages.debian.org/bookworm/libwebkit2gtk-4.1-dev) and [GTK development](https://packages.debian.org/bookworm/libgtk-3-dev); exact versions/hashes were archived with the download index.
