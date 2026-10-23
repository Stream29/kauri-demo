# Task Tree

- `Inspect static library build and host hooks`()
- `Build isolated static archives and Kotlin consumers`()
- `Exercise Mac offscreen UI and assess policy gaps`()
- `Record platform coverage and clean owned resources`()

# Details

- Test the pinned webview 0.12.0 static C API as a unified host, without replacing production adapters.
- Attempt macosArm64, mingwX64, linuxX64, linuxArm64. Distinguish archive compilation, executable linking, runtime execution, and application integration.
- Build on the Mac only, serialized and resource-limited. Use the Gradle helper if Gradle is needed; do not disturb existing daemons.
- Mac GUI checks must use an offscreen caller-owned window before `webview_create`; never create a visible library-owned window and hide it afterwards.
- Preserve typed kRPC and embedded resources for any application-integration claim. Do not call a separate-server or browser-only probe a standalone Kauri release.
- Archive reusable evidence under ignored `build/review/`; remove temporary builds and owned processes. No commit or production architecture switch.

## Results

- Four pinned upstream static archives and Kotlin C API consumers linked. Mac's application probe reuses existing backend klibs and frontend object, with no separate server executable.
- Mac standalone smoke: actual Canvas 110 colors, Native service, Unicode note/validation, Flow and native navigation/popup guards pass. The same binary separately passed seven backend checks in serve mode.
- First policy probe used reserved port 9 and did not observe navigation; corrected to the same test server under a foreign Host and passed. No upstream defect inferred.
- Linux x64 no-GUI version query passed with DISPLAY/WAYLAND_DISPLAY unset, including `LD_BIND_NOW=1`. Windows and Linux arm64 did not execute; Linux GUI and library-owned visible windows were not tested.
- Linux SDK: 37 Debian packages per architecture, checked against HTTPS index SHA-256 and extracted only into scratch. No system installation. Final consumers still need target `.so` link inputs; a lone prebuilt `.a` is insufficient for this Mac cross-link.
- Artifacts: `build/static-webview-spike/` on local/Mac, with hashes, headers/license, screenshot and smoke results. Production release hashes remain valid.
- Report: `resources/static-webview-spike.md`; agy Flash completed independent read-only review. Adopted coverage/SDK/delegate clarifications; rejected the unsupported claim that replacing UIDelegate necessarily breaks unrelated messaging and window lifecycle.
- Archived probe sources and build/runtime logs under ignored `build/review/`; removed scratch SDK and probe directories on local/Mac.
- Stopped only the spike-owned idle Gradle daemon (12358), verified ownership before termination, and released the project lock.
- Production source/build logic unchanged. No Git commit.
