# Task Tree

- `Verify Native, Wasm, and kRPC compatibility`()
- `Initialize repository and shared context`()
- `Implement standalone binary, shared Native service, and four platform hosts`()
- `try {`
  - `for (target in listOf(macosArm64, mingwX64, linuxX64, linuxArm64)) {`
    - `Cross-build standalone executable on remote Mac`(target)
    - `Record native runtime validation or unavailable runner`(target)
  - `}`
  - `Run native tests and offscreen WebKit end-to-end smoke`()
  - `Measure release executables and record evidence`()
- `} finally {`
  - `Stop owned test processes and remove temporary data`()
- `}`
- `Review documentation and hand off runnable prototype`()

# Details

- Requested: Tauri-like Kotlin/Native backend, Compose Wasm frontend, and kRPC communication.
- Confirmed follow-up: one self-contained executable; directly running it must open the application window.
- Confirmed packaging matrix: macosArm64 + mingwX64 + linuxX64 + linuxArm64.
- Source: `/home/stream/ACodeSpace/demo/Kauri`.
- Build mirror: `stream@macbook:~/ACodeSpace/demo/Kauri`.
- Do not run Gradle locally or take over shared GUI windows.
- Target system WebViews are runtime dependencies: WKWebView, WebView2, and WebKitGTK 4.1.
- Evidence and remaining limitations belong in `README.md` and `resources/validation.md`.
- Context workflows are pinned Git submodules in `.agents/skills/`.
- Initial Node download stalled; canceled only this task's client and switched to preinstalled nvm Node 24.12.0.
- All four release targets linked. Mac offscreen UI capture (110 sampled colors), typed kRPC round-trip / Flow, four Native tests, and seven boundary checks passed.
- Windows uses webview 0.12.0's source loader because the official MSVC static loader is ABI-incompatible with MinGW. PE imports require no companion loader or C++ DLL.
- linuxX64 non-GUI startup passed; Windows, Linux GUI, and Linux arm64 runtime checks remain explicit runner gaps, not claimed passes.
- Final artifacts and SHA-256 values are recorded in `resources/validation.md`; local copies are under `build/release/`.
- Current remote logs: `build-release.log`, `build-windows-host.log`, and `build/smoke/`.
- An external agent supplied Windows actuals; main-session review corrected string interop, secure token generation, bounded reads, atomic writes, and smoke URL handling.
- Documentation passed ablation, source/intent consistency, and whole-document review. No Git commit was requested or created.
