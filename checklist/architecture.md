# Confirmed migration target

- Replace the third-party `webview/webview` wrapper with a minimal Kotlin/Native host calling AppKit/WKWebView, Win32/WebView2, and GTK/WebKitGTK directly.
- The user accepts maintaining Windows Runtime loading through `CreateWebViewEnvironmentWithOptionsInternal`. Keep discovery, trusted-path loading, entry resolution, diagnostics and module lifetime in a small internal implementation; this risk is not a migration blocker.
- Remove the wrapper's C/C++ implementation, patches and static-library build pipeline when its replacements are validated. SDK declarations and generated cinterop remain allowed; preserve required source attribution when porting loader logic.
- Retain one window, Compose-owned routing/dialogs, minimum document/popup guards, isolated smoke mode and four independently copyable executables. Do not build a general desktop framework.
- The implementation and validation status is recorded in `kanban/executable/2026-10-21-direct-native-host.md` and `resources/validation.md`; this checklist is the post-migration boundary.

# Implemented runtime

- Release targets: macosArm64, mingwX64, linuxX64, and linuxArm64.
- A single Kotlin/Native executable owns one window/system WebView alongside the embedded Ktor CIO server. No C/C++ host, webview/webview submodule, wrapper archive, or wrapper patch remains in the build.
- macOS calls AppKit/WKWebView directly. MinGW calls Win32 and WebView2 COM vtables generated from the official SDK header; Linux calls GTK/WebKitGTK through a small declaration-only cinterop header.
- Windows discovers the runtime below Microsoft EdgeWebView installation roots, loads an absolute `EmbeddedBrowserWebView.dll` path with restricted dependency search, and calls the five-argument `CreateWebViewEnvironmentWithOptionsInternal`. Loaded runtime modules are cached for process lifetime; do not unload them under callback stacks. This deliberately removes an unnecessary unload protocol. The accepted internal-entry compatibility risk remains explicit.
- Windows callbacks implement the actual `QueryInterface`/`AddRef`/`Release` contract for each supported WebView2 handler IID, keep `StableRef` state until final release, and catch failures before returning through the C ABI.
- Windows and Linux require their system WebView runtime; do not bundle Chromium or a companion loader DLL.
- The executable must open its own window. Link all frontend assets into its Mach-O, PE, or ELF image; no adjacent application resource directory is required.
- The UI is Compose Multiplatform compiled to WasmJs; there is no Compose Desktop/JVM runtime.
- Compose Wasm owns application routing and in-content dialogs. Native popup/multiwindow support is out of scope for this release; document navigation guards remain a host security boundary, not an application router.
- `app/shared` contains application DTOs and the `@Rpc` service contract, compiled for WasmJs and all four Native targets.
- Application requests and server streams use kotlinx-rpc over loopback WebSockets.
- Bind only to `127.0.0.1`, use an ephemeral port, and generate a fresh capability per launch.
- Pass the capability to the local UI in the URL fragment; require it in the RPC endpoint.
- Check the WebSocket Origin, restrict WebView navigation to the app origin, and serve embedded frontend assets directly from memory.
- Native APIs are capability-specific: system information and one app-owned note, not arbitrary filesystem or shell access.
- App shutdown must stop the server and cancel its coroutines.
- Smoke mode isolates browser/application data; it must not modify real notes. Native GUI tests require an explicitly owned graphical runner and use ordinary windows, never the user's shared desktop.
- Browser contexts are always disposable: non-persistent WKWebsiteDataStore, ephemeral WebKitGTK context, and a fresh temporary WebView2 user-data directory with orderly cleanup. Business persistence and authorization stay in Native application code; no browser profile migration or deletion of old profiles is performed.
- Native offscreen positioning/activation is removed; Compose UI testing and native-host integration are separate test layers.

# Module boundaries

- Confirmed API direction: `framework` provides independently usable atomic capabilities; the caller owns the composition root. It is a library, not an application-orchestration framework.
- Public capabilities are streaming `openEmbeddedResource(path): kotlinx.io.Source?` and `WebViewWindow(url, title)` with scoped `run()` and thread-safe `requestClose()`. Resource payloads must not be materialized as whole-file ByteArrays by the framework or Ktor integration. The caller owns service composition and source closing.
- Fixed disposable-context implementation is complete in `kanban/done/2026-10-22-ephemeral-browser-context.md`; temporary Windows data can still touch disk, and GUI/runtime cleanup requires an owned runner.
- Offscreen ablation is complete in `kanban/done/2026-10-22-remove-offscreen.md`; GUI coverage must be re-established on an owned runner, not claimed from older offscreen binaries.
- Streaming implementation is recorded in `kanban/done/2026-10-22-streaming-resources.md` / `resources/streaming-resources.md`; current release evidence is in `resources/validation.md`. Framework may depend on standard kotlinx-io, but not Ktor/kRPC or a custom server/stream protocol.
- Window safety, thread/FFI ownership and native cleanup stay inside the window. Existing loopback/capability/Host/Origin/path security stays intact in app-owned server integration; there is no new Kauri server wrapper.
- The framework must not require a built-in `application {}` DSL, service factory protocol or all-in-one launch function. Any demo orchestration belongs in `app/desktop`; the framework never imports an application service contract.
- Fresh-source implementation of `resources/atomic-framework-api.md` is complete in `kanban/done/2026-10-22-atomic-framework-api.md`. Its rewrite evidence and subsequent streaming/offscreen baselines are historical; current validation and runner gaps are in `resources/validation.md`.
- `app/desktop` owns the executable entry point, RPC service implementation, and native business APIs, including system information and note persistence.
- `app/shared` and `app/frontend` remain application-specific. The framework must not depend on any `app` project.
- Use ordinary KMP module scripts and plugin-provided tasks. Linux SDK inputs are prepared separately; cinterop and linker settings describe their use. There is no included build, root aggregation task, or custom task class.
- Frontend distribution production and executable linking must have explicit producer dependencies. Keep only the resource embedding transformation needed for the single-file requirement; release scripts own copying, signing and checksums.
- Framework code compiles without an application; final single-file linking requires application assets. The framework remains repository-local, not a published SDK.
- The platform host implementations are direct Kotlin/Native sources. Historical extraction/spike records are evidence only and are not runtime dependencies.

# Validation gates

- Compile all four Native targets and the production Wasm frontend on the remote Mac.
- Run application contract/storage tests and framework path/configuration tests.
- Exercise actual Wasm → kRPC → Native calls and a server Flow in WKWebView.
- Record compilation and native runtime validation separately for each target; unavailable runners are explicit validation gaps.
- Test unauthorized RPC, foreign origins, and static path traversal.
- Measure the standalone release executable and record the contribution of embedded web assets.
- Separate measured facts from size, development-experience, or portability hypotheses.
- Follow `checklist/build.md` for build ownership, cache behavior, repository policy, and release gates.
