# Historical validation before the static-host migration

Date: 2026-10-21. These results used convention plugins/custom platform adapters. Old hashes do not certify current artifacts; see [current validation](validation.md).

## Build environment

- stream@macbook, Apple Silicon, macOS 26.5.1, Xcode.
- Kotlin 2.4.0, Compose 1.11.1, kRPC 0.10.4, Ktor 3.3.0, Gradle 9.6.1.
- Helper explicitly reused detected daemon JVM/options, GraalVM 21.0.8, max two workers. nvm Node 24.12.0.
- All Gradle, native links and C/C++ cross-compilation ran on Mac.
- Removing binaries/assets/host objects caused four real release links and three host producers: 1m 51s. Repeat: 781ms, 108 tasks (6 executed / 102 up-to-date), configuration-cache reuse. Deleted archive/SDK headers restored offline from cache. Warm-cache observations, not cold-build/DX comparison; bootstrap/Mac-only evidence belonged to earlier governance.

## Standalone files

| Target | Path under build/release | Bytes | MiB | Actual coverage |
| --- | --- | ---: | ---: | --- |
| macosArm64 | macosArm64/Kauri | 16,993,824 | 16.21 | Mach-O arm64; Native tests, offscreen WKWebView, Canvas/kRPC end-to-end |
| mingwX64 | mingwX64/Kauri.exe | 19,524,096 | 18.62 | PE32+ x64 GUI/system import audit; not executed |
| linuxX64 | linuxX64/Kauri | 18,704,512 | 17.84 | ELF x86-64/dependency audit; low-priority workstation --licenses, no GUI |
| linuxArm64 | linuxArm64/Kauri | 18,172,888 | 17.33 | ELF aarch64/dependency audit; no target runner/execution |

- Mac strip -x/ad-hoc signed. Windows/Linux kept Native release-link defaults.
- Sizes exclude system WebView/libraries, caches and notes; they are not runtime memory.
- Six web assets packed to **12,971,920 bytes (12.37 MiB)**: Wasm 8,652,729/3,774,033 bytes, JavaScript 539,079 bytes, plus HTML/notices.

## Module review

- Modules: :framework, :app:shared, :app:desktop, :app:frontend. Framework source/dependencies did not reference application contracts/services.
- Standalone framework compile/three Native tests passed; its graph had no application compile/link. Global SwiftPM lock metadata was KGP orchestration, not business-code dependency.
- Historical DesktopApplication provided identity/title/service factory; LocalServer used a routing callback for typed kRPC.
- npm workspace names/lock entries moved with module paths; obsolete generated workspaces were cleaned. Final normal release did not bypass lock validation.
- Declarative frontend/host object variants carried producers; links tracked files, not only names/paths.
- Then-current build-logic had four precompiled conventions/typed tasks, replacing handwritten plugin classes/root apply-false list/single-use extension while retaining variants/cache/manifests. See [build-review](build-review.md).

## Passed checks

- Native 6: contract 2, persistence 1, framework boundaries 3; zero failures/errors.
- Build-logic 14 tests/plugin validation and policy 11; cache/offline SDK/Mac-only/bootstrap-reuse were exercised.
- Mac standalone smoke copied one executable into an empty directory, no adjacent web assets.
- Actual Compose ShadowRoot Canvas capture: 64×64 sample, **110 colors**, not blank.
- Real Wasm→Native typed kRPC: system info, Unicode save/read, rejected oversize without mutation, Flow 1/2/3.
- HTTP seven: health 200; foreign Origin/Host, missing WebSocket Origin and invalid capability 403; raw/encoded traversal 404.
- Four downloaded artifacts passed sha256sum -c SHA256SUMS.
- PE imported system DLLs only, no WebView2Loader/libstdc++/libgcc companions.
- Final linuxX64 --licenses ran at low priority, with no local Gradle/window.
- Mac codesign --verify --strict passed ad-hoc signing, not notarization.
- Test processes/temp standalone/data directories were cleaned; only the task-created idle daemon stopped.

Offscreen tests did not activate windows. A test timer frame clock bypassed occlusion throttling; ordinary windows used the native clock. This proved rendering/communication, not normal-window FPS/latency.

```text
PASS: Compose painted (110 sampled colors); Native systemInfo;
Unicode note round-trip; rejected oversize; Flow 1,2,3; unauthorized RPC 403
```

## Historical checksums

```text
7c945e79acd34ea041392fc81c4827a60ef6567f96727e5518a6459a27c2f9b3  macosArm64/Kauri
e015c46681c41507fcd6d39a9fe495105415df6f52176384fc64fda409c7a1d4  mingwX64/Kauri.exe
290dd4285194f9dc42accf33371cf92e9e3a91ab4f0734faba24bb4cd68b9e73  linuxX64/Kauri
60f5c4a2a58ae8cdc9c1395c70fb3aa42314d61f525e5a0aad7195b24ace90b5  linuxArm64/Kauri
```

## Evidence locations

- Local/Mac build/release: four files/checksums; build/smoke/window.png, result.txt and process.log: actual runtime capture.
- build/review/simplify-{check,release,reuse,restore,policy}.log: this iteration; older log index in build-review.md. .gradle/kauri held current helper log/ownership, excluded from Git.
- Native XML: the three modules' build/test-results; removed build-logic XML archived to build/review/legacy-build-logic-tests.xml, not a current test entry.
- Artifacts, raw logs, capability URLs and actual temporary notes were excluded from Git.

## Unverified or unimplemented

- Windows/Linux arm64 execution and both Linux GUI targets.
- Required system WebView2 or GTK 3/WebKitGTK 4.1 and WasmGC support.
- Updates/installers/Developer ID/Authenticode/notarization/hot reload/production permission policies.
- No equivalent Electron/Tauri/Compose Desktop comparison; no guaranteed smaller size, better DX or performance.
