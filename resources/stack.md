# Stack evidence

Retrieved 2026-10-20. These are implementation-oriented summaries, not upstream quotations.

- 2026-10-21: the user authorized and the task implemented the direct Kotlin/Native system-host migration. Current boundaries and measured results are in [validation.md](validation.md) and [minimal-static-host.md](minimal-static-host.md); the candidate discussion below is historical.

| Source | Finding | Scope |
| --- | --- | --- |
| [kotlinx-rpc platform support](https://github.com/Kotlin/kotlinx-rpc) | kRPC supports WasmJs and Apple targets. | Library platform support; not proof of this app. |
| [kRPC Ktor integration](https://kotlin.github.io/kotlinx-rpc/krpc-ktor.html) | Typed services use Ktor WebSocket transports; clients use `withService`. | Transport/API design. |
| [kRPC versions](https://kotlin.github.io/kotlinx-rpc/versions.html) | Compiler plugin and Kotlin versions must match a published combination. | Pin all versions; validate by compiling. |
| [kRPC changelog](https://github.com/Kotlin/kotlinx-rpc/blob/main/CHANGELOG.md) | 0.10.3 fixes Native release dispatch; 0.10.4 updates Kotlin to 2.4.0. | Choose 0.10.4, not an older release with known Native issues. |
| [Kotlin Wasm browser configuration](https://kotlinlang.org/docs/wasm-configuration.html) | WebKit 18.2+ supports the required Wasm features. | Installed WKWebView must satisfy this; macOS version alone is not sufficient. |
| [Ktor server engines](https://ktor.io/docs/server-engines.html) | CIO is the candidate multiplatform server engine. | Verify native linking and runtime. |
| [Kotlin/Native hosts](https://kotlinlang.org/docs/native-target-support.html) | Apple Silicon macOS can build all four requested targets. | Cross-compilation is not target-native runtime testing. |
| [WebView2 deployment](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/distribution) | The installed Runtime is a system dependency. | Kauri resolves a trusted absolute runtime module and calls the accepted internal entry directly; it does not ship WebView2Loader.dll. |
| [WebKitGTK API](https://webkitgtk.org/reference/webkit2gtk/stable/) | GTK/WebKitGTK provides an embeddable system WebView. | Require WebKitGTK 4.1 with WasmGC support, not just an arbitrary old package. |
| [Kotlin Node setup](https://kotlinlang.org/docs/js-project-setup.html) | The Gradle Node environment can reuse an existing Node installation. | Use nvm Node 24.12.0; do not stall on a redundant download. |

## Pinned implementation

- Kotlin 2.4.0; Compose Multiplatform 1.11.1; kotlinx-rpc 0.10.4.
- Ktor 3.3.0, matching the published RPC integration dependency.
- Gradle 9.6.1; remote JDK selected explicitly by the build helper.
- Remote machine: Apple Silicon, macOS 26.5.1, Xcode installed.
- WebView2 SDK: Microsoft.Web.WebView2 1.0.3650.58 from NuGet; SHA-256 `911a472128c82ac8baa0c486c23342cc9dd6e7dc50d754e676726642ca065c60`.
- The direct Windows loader is implemented in Kotlin/Native. It searches only Microsoft EdgeWebView install roots, loads an absolute `EmbeddedBrowserWebView.dll` path, verifies `CreateWebViewEnvironmentWithOptionsInternal`, and keeps the HMODULE alive through COM callback release. The internal-entry version risk is accepted by the user.
- This loader supports installed stable WebView2 Runtime, not Edge preview channels or environment-variable runtime overrides. Its runtime discovery uses upstream implementation details; this is a compatibility risk to test on Windows.
- Linux shell dynamically loads GTK 3 / WebKitGTK 4.1; the app's own shell code and web assets are embedded, not extracted alongside the executable.
- Compose UI 1.11.1's published [Wasm source archive](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-wasm-js/1.11.1/ui-wasm-js-1.11.1-sources.jar) creates the rendering canvas inside an open ShadowRoot. Canvas smoke capture must traverse shadow roots.
- WKWebView's [inactive scheduling preference](https://developer.apple.com/documentation/webkit/wkpreferences/inactiveschedulingpolicy-swift.property?language=objc) controls background work. Native snapshot APIs can omit GPU canvas content; smoke capture uses the actual canvas and validates sampled color variation instead.

## Native host candidates

Reviewed 2026-10-20 as historical candidate research. The direct Kotlin/Native migration below is now implemented; this table is not an active architecture proposal.

| Candidate | Evidence | Kauri assessment |
| --- | --- | --- |
| [webview](https://github.com/webview/webview) | Unified C API for creating windows, navigating, running the GUI loop, and destruction; Cocoa/WebKit, Win32/WebView2, GTK/WebKitGTK backends; static-library target. Version 0.12.0 is already pinned locally. | Lowest-change candidate: consume the public C API with Kotlin/Native cinterop instead of maintaining three hosts. Navigation-policy hooks, lifecycle, Linux SDK/sysroots, and all four build targets still need validation. |
| [Wry](https://github.com/tauri-apps/wry) + [Tao](https://github.com/tauri-apps/tao) | Tauri-maintained Rust WebView and windowing libraries. Wry's [builder API](https://docs.rs/wry/0.57.0/wry/struct.WebViewBuilder.html) includes navigation, popup, permission, and custom-protocol handlers. | Broader host integration to evaluate if accepting a Rust toolchain and a small C ABI bridge. Business logic can remain Kotlin/Native and application communication kRPC. |
| [Saucer](https://github.com/saucer/saucer) | Window/WebView abstraction, C interface, embedding, and desktop extensions. The [8.0.5 build configuration](https://github.com/saucer/saucer/blob/v8.0.5/CMakeLists.txt) requires C++23, uses GTK4/WebKitGTK 6.0 on Linux, and warns about MinGW SDK compatibility. | Feature-rich alternative, but not a drop-in fit for the current old MinGW sysroot and GTK3 build setup. Validate compatibility before selection. |

- Library reuse reduces handwritten OS integration; it does not remove system WebView dependencies or the need for application security and target-native tests.
- Proposed boundary: the library owns platform windows/WebViews; Kauri owns the Kotlin-facing API, embedded assets, capability policy, and typed kRPC services.
- 2026-10-21: the static-webview spike was superseded by the direct-host implementation. Its four archives remain historical evidence only; current release hashes and runtime coverage are in [validation.md](validation.md).

### Wry + Tao trade-offs

Checked Wry 0.57.0 and Tao 0.37.1 documentation on 2026-10-20; still research only.

- [Tao WindowBuilder](https://docs.rs/tao/0.37.1/tao/window/struct.WindowBuilder.html) exposes window constraints, decorations, visibility, fullscreen, and theme configuration. [EventLoopProxy](https://docs.rs/tao/0.37.1/tao/event_loop/struct.EventLoopProxy.html) queues user events to the GUI loop; a Kauri bridge could route Native coroutine requests through it.
- Wry's builder exposes navigation/popup/permission/download hooks, initialization scripts, and custom protocols. These can replace platform-specific control plumbing, not Kauri's policy decisions.
- [Custom protocols](https://docs.rs/wry/0.57.0/wry/struct.WebViewBuilder.html#method.with_custom_protocol) could serve embedded assets without static HTTP routes. This does not remove kRPC's WebSocket server. Custom-protocol origins differ by platform, so the current exact-origin validation must be redesigned and tested if adopting this path.
- The added cost is a Rust build chain for producing/upgrading the host library, plus a C ABI boundary with explicit ownership, threading, error, and shutdown rules. Application developers need not run Cargo if they consume prebuilt host libraries. Single-file linking and all four target builds remain validation work; no size, performance, or reliability superiority has been measured.
- Comparison clarification: webview 0.12.0 already manages window/application lifecycle when creating its own window, supports GUI-thread dispatch, script initialization/evaluation, and exposes native handles. Embedded assets and single-file packaging are already Kauri-owned capabilities, not exclusive benefits of Wry/Tao.
- The meaningful distinction is public cross-platform API coverage: Wry offers dedicated navigation/permission/popup/custom-protocol handlers; webview's smaller public C API may require platform-specific native-handle extensions for those policies. This is a difference in integration work, not proof that webview cannot implement the behavior or that Wry is inherently more reliable.

### Prebuilt static host distribution

- The [Rust Reference](https://doc.rust-lang.org/reference/linkage.html#r-link.staticlib) specifies `staticlib` for linking Rust and its Rust dependencies into non-Rust applications using a foreign linker. Use this format, not an `rlib`.
- Proposed delivery: a versioned C header and one prebuilt static host library for each Kauri target. The Kotlin/Native application build downloads/verifies the matching artifact and links it without invoking Cargo. Only host-library production or upgrades need the Rust toolchain.
- For Windows, produce a Windows GNU/MinGW-compatible library and audit CRT/system-library requirements; do not assume an MSVC `.lib` matches mingwX64. System WebView dependencies remain, and the final executable still needs native dependency auditing.
- This is a packaging design, not a completed Wry/Tao integration or four-target linkage validation.

### Reliability and failure attribution

Reviewed official Tauri documentation on 2026-10-20; no comparative defect-rate study was performed.

- [Tauri architecture](https://v2.tauri.app/concept/architecture/) includes runtime glue, command APIs/code generation, asset/config handling, tooling, bundling, and plugins; the full framework is not just a window/WebView wrapper.
- [Tauri's Linux graphics guide](https://v2.tauri.app/develop/debug/linux-graphics/) documents WebKitGTK/driver/compositor failures, including blank windows and WebGL/canvas slow paths. This establishes concrete compatibility risks, not their prevalence across all users.
- Using Wry/Tao without the full Tauri framework omits Tauri-specific framework code but retains its window/WebView adapter and system-engine risks. Using webview or handwritten hosts can still encounter system WebView issues; Kauri's Canvas/Skia rendering needs target-native graphics tests.
- Static host libraries pin the host implementation, not the installed WebView engine, display server, or GPU driver. Bug counts and issue counts alone cannot establish relative reliability.

### Focused Kotlin/Native launcher candidate

Discussed 2026-10-20 after the user proposed direct system-API integration for Kauri's fixed use case. This is a proposal, not authorization to rewrite the hosts or a confirmed architecture change.

- 2026-10-21: user authorized implementation after feasibility exploration. [Probe results and loader trade-offs](pure-native-feasibility.md) remain evidence for ABI details; the direct hosts now replace the old wrapper and Windows/Linux GUI remain explicit runtime gaps.

- Proposed scope: one top-level window, one system WebView, a trusted loopback URL, navigation restrictions, resize, and shutdown coordination. Keep embedded HTTP assets and kRPC; do not add a JavaScript/native business bridge, custom protocol, generic multi-window framework, or browser-style functionality.
- Before module extraction, the macOS host already called AppKit/WKWebView from Kotlin/Native. At that review point, host-entry files were 191 Kotlin lines on macOS, 279 C lines on Windows, and 139 C lines on Linux, including smoke/diagnostic helpers; these historical counts are not a maintenance-cost estimate. Hosts now live under `framework/`; the confirmed module boundary is in `checklist/architecture.md`.
- [Kotlin/Native interop](https://kotlinlang.org/docs/native-c-interop.html) supports platform Win32/Apple bindings and generated C bindings. Prefer official headers and generated bindings over handwritten function signatures or ABI structures. Linux can share one implementation across x64/arm64 with a fixed GTK/WebKitGTK API family, while retaining system runtime-version caveats.
- Windows is the main feasibility gate: WebView2 uses COM and an STA GUI thread with a live message pump; callbacks, reentrancy, reference ownership, and asynchronous shutdown remain even when written in Kotlin. See [Microsoft's threading model](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/threading-model).
- Windows loader/MinGW ABI compatibility is a separate problem from launcher language. The current build no longer uses the pinned webview source loader. The Kotlin implementation retains the accepted internal-entry compatibility risk and still requires a Windows runtime runner for GUI validation.
- The value proposition is a smaller owned feature set and one implementation language, not inherently greater reliability than Wry/Tao or webview. Preserve real target-native tests and explicit unsupported-feature boundaries.

### Kotlin/Native interop cost assessment

Reviewed 2026-10-20 against Kotlin 2.4.0, matching the project. This is source-based analysis, not a benchmark or authorization to change the launcher.

- The [2.4.0 interop runtime](https://github.com/JetBrains/kotlin/blob/v2.4.0/kotlin-native/Interop/Runtime/src/main/kotlin/kotlinx/cinterop/Utils.kt) implements `MemScope` through `ArenaBase`, whose default allocation parent is `nativeHeap`. Scope exit frees allocations; it does not imply C stack allocation. Native heap storage and Kotlin GC-managed objects are distinct.
- The same runtime shows string encoding/copying in `cstr`/`wcstr`, a `ByteArray` copy in `readValue`, and temporary placement in `CValue.useContents`. Actual optimized call costs require generated-code inspection or measurement; not every interop call follows these conversion paths.
- The [official interop documentation](https://kotlinlang.org/docs/native-c-interop.html) distinguishes existing pointers from temporary `CValuesRef` copies and documents pinned array access. Pinning can avoid a payload copy, but pointers must remain within their valid lifetime; asynchronous APIs need separate ownership management.
- Kauri's Compose/Wasm rendering stays inside the system WebView. The launcher creates and controls native windows/WebViews; it does not transport each frame's drawing commands or pixels through Kotlin/Native. Interop overhead is therefore not an identified architectural blocker for this narrow launcher; this is an inference, not measured performance evidence.
- Current embedded-resource reads do copy native archive bytes into `ByteArray` before HTTP serving. That is a resource-loading path, not a zero-copy or per-frame path; startup allocation/latency still needs measurement.
- If implementing more of the launcher in Kotlin, reuse native message structs/buffers outside loops, avoid repeated string/value conversions in callbacks, and batch bulk work. Profile release builds for resize/input latency, allocations, startup and repeated resource loads. Keep a small native helper only if an actual hot path justifies it.

### Build governance references

Reviewed 2026-10-20 for the pinned Gradle 9.6.1 / Kotlin 2.4.0 build. Gradle's current web manual serves newer 9.x guidance; used APIs were also checked against the installed 9.6.1 API and actual builds, not used as a reason to upgrade the wrapper.

- Gradle recommends [included build logic and convention plugins](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html) and [declared cross-project artifacts](https://docs.gradle.org/current/userguide/how_to_share_outputs_between_projects.html). Kauri now uses typed repository-local plugins and attributed frontend/native-object configurations.
- [Configuration-cache requirements](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html) prohibit captured project/script state in task execution. Custom actions use managed inputs/outputs and injected execution services; portable and local-only transformations have separate cache policies.
- The [configuration-cache lifecycle](https://docs.gradle.org/current/userguide/configuration_cache.html) skips init scripts on a hit. The daemon guard therefore uses a configuration-observed `ValueSource`, rather than relying only on top-level init code or a post-build message.
- Kotlin 2.4.0's [EnvSpec](https://github.com/JetBrains/kotlin/blob/v2.4.0/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/EnvSpec.kt) documents an absent `downloadBaseUrl` for externally managed repositories. Its [Binaryen setup](https://github.com/JetBrains/kotlin/blob/v2.4.0/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/binaryen/BinaryenSetupTask.kt) defines the artifact-only Ivy coordinates/pattern. Kauri retains downloading and optimization while centralizing that endpoint in settings.
- Kotlin 2.4.0's [Binaryen default](https://github.com/JetBrains/kotlin/blob/v2.4.0/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/wasm/binaryen/BinaryenExtension.kt) is 125. The explicit catalog pin preserves it. Inspection of the official Compose 1.11.1 plugin artifact likewise confirmed its Material3 default is 1.9.0; direct catalog dependencies preserve that existing choice.
- Independent agy findings and verified dispositions are summarized in `resources/build-review.md`; build ownership and maintenance gates are in `checklist/build.md`.

### Build simplification references

Reviewed 2026-10-21 against the unchanged Gradle 9.6.1 wrapper.

- [Precompiled script plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_precompiled.html) derive plugin IDs from script filenames and support typed plugin DSL. Four scripts replace five hand-written plugin classes and their registration block; frontend-only setup stays in its module.
- [Configuration role factories](https://docs.gradle.org/current/userguide/declaring_configurations.html) express declaration, resolution, and publication separately. Kauri uses these APIs instead of manually setting role flags.
- [`apply false`](https://docs.gradle.org/current/userguide/plugins_intermediate.html) resolves a plugin without applying it to that project; it is not required merely because a build has subprojects or a version catalog. Kauri removed the duplicate root aliases, uses the included build's KGP classpath, and retains module aliases for other plugins.
- The current manual serves newer 9.x documentation. Compatibility is established by the pinned wrapper's actual compilation/tests, not an implicit wrapper upgrade.

## Hypotheses

- Sharing Kotlin types may reduce frontend/backend integration friction.
- Whether this architecture reduces the footprint of an equivalently featured Electron, Tauri, or Compose Desktop app still requires controlled comparison.
