# Kauri 0.1.0 — experimental preview

A Kotlin/Native desktop host, Compose Wasm frontend and typed kRPC backend in one executable per target. Application code owns the composition root; the framework exposes only window lifetime and streaming embedded resources.

## Downloads

| Asset | Executable after extraction | Required environment |
| --- | --- | --- |
| Kauri-0.1.0-macosArm64.tar.gz | macosArm64/Kauri | Apple Silicon macOS with a WasmGC-capable system WKWebView |
| Kauri-0.1.0-mingwX64.zip | mingwX64/Kauri.exe | Windows 10/11 x64 with a modern Microsoft Edge WebView2 Runtime |
| Kauri-0.1.0-linuxX64.tar.gz | linuxX64/Kauri | glibc x86-64, GTK 3, WebKitGTK 4.1 with WasmGC, required system libraries and a graphical session |
| Kauri-0.1.0-linuxArm64.tar.gz | linuxArm64/Kauri | glibc aarch64 and the same Linux GUI/runtime requirements |

- Each archive contains one executable, not a browser engine, JVM, Node.js or companion application resource directory.
- macOS is ad-hoc signed, not Developer ID signed/notarized. Windows is not Authenticode signed. OS security prompts or policy restrictions may apply.
- SHA256SUMS covers release attachments except itself; BINARY-SHA256SUMS records extracted executable hashes. SOURCE-SHA256SUMS and BUILD-INFO.txt tie tested source inputs to the release commit. VALIDATION.txt states actual test coverage.

## Included

- Native system information, atomic persistent notes and a typed Native Flow demonstration.
- Streaming openEmbeddedResource returning standard kotlinx-io Source, directly usable with Ktor respondSource and backpressure.
- WebViewWindow(url, title) with run/requestClose; no application DSL, profile/persistence options or native offscreen mode.
- Direct AppKit/WKWebView, Win32/WebView2 COM and GTK/WebKitGTK calls, without a handwritten C/C++ host or Rust toolchain.
- Disposable browser contexts. Native notes remain persistent; Windows can still use temporary disk storage, with bounded orderly cleanup.
- Fresh loopback launch capabilities, Host/Origin/path checks and native document/popup boundaries.
- English first-party documentation and HTML language metadata; Unicode fixtures retain emoji/accent coverage.

## Actual validation

- Four real release links on the remote Mac.
- Forced Mac Native tests: 14 passed, zero failures/skips.
- Resource-pack tests: 4 passed. Build-policy checks: 12 passed.
- Mac licenses CLI, seven HTTP security checks, interrupted-client/reopened chunked byte verification and writer backpressure/cancellation passed.
- Configuration-cache reuse and unchanged-link up-to-date checks passed.
- **GUI NOT RUN for this release.** No owned graphical runner was available. Native browser creation/context isolation, actual Canvas/kRPC/Flow/native policy callbacks and close/failure cleanup need fresh target execution.
- **Windows/Linux are cross-compile/link/format/dependency-audit coverage only, not target CLI/GUI passes.** Historical Mac GUI evidence does not certify these binaries.

| Executable | Bytes | SHA-256 |
| --- | ---: | --- |
| macosArm64/Kauri | 17,043,248 | `a20bfa9a237b63edf00eb22e378ecec3f1839b5eed3f7f57bd62fad0eefd5851` |
| mingwX64/Kauri.exe | 19,701,760 | `0530354c172446a8fa41f0eaf00cae2dacf72ee2035a250f7d59d6f38e33febd` |
| linuxX64/Kauri | 18,801,520 | `eca3ac9666a7dec2938fe8deca5eb37bb98be619042e6d2f271d7d0b11a56507` |
| linuxArm64/Kauri | 18,270,232 | `69a86861f6a0701f1cd686d7f46785b20674d93d4ecacb04144cdf9a4fa9ab82` |

## Known limits

- Windows deliberately uses CreateWebViewEnvironmentWithOptionsInternal. This internal, version-sensitive initialization interface can change across Runtime versions; the accepted risk is not hidden by single-file packaging.
- Linux dynamically requires GTK/WebKitGTK and associated system libraries; missing dependencies can fail before main. A sufficiently recent WasmGC-capable engine is required.
- No automatic updates, installers, production signing/notarization or universal system-WebView compatibility.
- No measured performance advantage over Tauri/Electron/Compose Desktop, no memory-only Windows storage guarantee, and no cross-machine reproducible-build claim.

See README.md, resources/validation.md and checklist/build.md for setup, evidence boundaries and build ownership rules.
