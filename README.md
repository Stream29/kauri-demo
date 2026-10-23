# Kauri

**Native roots. Wasm canopy.**

A Tauri-like desktop experiment: a Kotlin/Native backend, a Compose Wasm interface, and typed kRPC over shared Kotlin contracts.

## Delivery model

- **One standalone executable per target**: running it opens the application window.
- Targets: macosArm64, mingwX64, linuxX64 and linuxArm64.
- Compose Wasm, Skia and frontend assets are linked into the Mach-O, PE or ELF image. No adjacent application resource directory is required.
- Uses the system WebView; does not bundle a JVM, Node.js or Chromium. Build and runtime coverage are recorded separately below.

```text
Kauri executable
├── Kotlin/Native
│   ├── direct Kotlin/Native host → platform window + system WebView
│   ├── embedded frontend assets → loopback Ktor CIO
│   ├── typed kRPC service → native APIs
│   └── app-owned UTF-8 note
└── WKWebView / WebView2 / WebKitGTK
    └── Compose Wasm → generated kRPC client → shared @Rpc interface
```

## Demo features

- **System information** through Foundation, Win32 or POSIX APIs.
- **Persistent notes**: Compose input → typed saveNote() → atomic native file writes.
- **Native events**: a Native Flow<Pulse> updates Compose state over kRPC every second.
- **Security boundaries**: an ephemeral loopback port, a fresh launch capability, Host/Origin checks and same-origin document navigation.

| Platform | Note location |
| --- | --- |
| macOS | `~/Library/Application Support/Kauri/note.txt` |
| Windows | `%LOCALAPPDATA%\Kauri\note.txt` |
| Linux | `$XDG_DATA_HOME/Kauri/note.txt`, default `~/.local/share/Kauri/note.txt` |

The prototype exposes neither arbitrary filesystem access nor shell execution.

## Application and framework

```text
app/
├── shared/       # Application @Rpc contract, DTOs and validation
├── frontend/     # Compose Wasm UI
└── desktop/      # Executable entry, services, system information and native notes
framework/       # Independent native window/WebView and embedded resource access
```

- Dependencies: app/frontend → app/shared; app/desktop → app/shared + framework. The framework never depends on the application.
- The caller owns the composition root: create Ktor CIO, register typed kRPC services, serve assets and pass the URL to the window.
- The framework offers two atomic capabilities, without an application DSL, server wrapper, Host/Controller API or business JS bridge:

```kotlin
fun openEmbeddedResource(path: String): kotlinx.io.Source?

class WebViewWindow(url: String, title: String) {
    fun run()
    fun requestClose()
}
```

- Construction acquires no native resources. Main-thread run() creates, runs and cleans up the window; its return is the completion boundary. Thread-safe requestClose() requests shutdown without exiting the process or stopping the server.
- Resource readers have independent cursors and must be closed with use. Native reads copy at most 8 KiB at a time, not the complete payload. The Ktor integration is `source.use { call.respondSource(it, contentType) }` without contentLength: pinned Ktor's explicit-length branch accumulates that length before flushing. See [streaming implementation](resources/streaming-resources.md).
- The application stops Ktor in its own try/finally and owns capability/Host/Origin/RPC/path checks. Native navigation, popup rejection and FFI ownership stay inside the window.
- Browser contexts are disposable; business persistence belongs to Native code. There are no profile, persistence or offscreen options. Windows still uses a temporary disk directory with shutdown cleanup, not a memory-only guarantee.
- macOS calls AppKit/WKWebView, Windows calls Win32/WebView2 COM and Linux calls GTK/WebKitGTK directly. Compose owns page routing and in-content dialogs.
- API rationale: [atomic capability specification](resources/atomic-framework-api.md). The former runDesktopApplication and host implementations are removed, with no compatibility entry.

## Build

Project builds run on `stream@macbook:~/ACodeSpace/demo/Kauri`, not the user's Linux workstation.

```bash
# Synchronize from the repository root without replacing remote build caches.
rsync -az --exclude=.git --exclude=.gradle --exclude=build --exclude=.agents \
  ./ stream@macbook:ACodeSpace/demo/Kauri/

ssh stream@macbook \
  'cd ~/ACodeSpace/demo/Kauri && bash scripts/prepare-native-mac.sh && bash scripts/release-all-mac.sh'
```

- Use scripts/gradle-mac.sh. It reads the wrapper version, explicitly reuses an idle daemon's actual JVM/options and limits work to two workers. Identity validation also runs on configuration-cache hits.
- Without a daemon, the helper bootstraps one using an explicit JDK. The JDK runs Gradle, not the application.
- Node 24.12.0 is managed by nvm (.nvmrc); Gradle does not download another Node.
- Versions live in gradle/libs.versions.toml; native compatibility inputs live in gradle/native-toolchains.properties. This work does not upgrade application dependencies.
- Ordinary KMP scripts declare targets, binaries, cinterop, dependencies and linking. No build-logic included build, custom task classes or publishing plugin remains.
- The root keeps one KMP apply false declaration to share the plugin classpath and avoid an observed KGP SwiftPM build-service classloader conflict. There is no root base/check/assemble wrapper.
- scripts/prepare-native-mac.sh explicitly prepares Native toolchains and verified Linux GTK/WebKitGTK link inputs using KGP's distribution task and Native dependency check. It compiles no C/C++ host/wrapper. Pass macosArm64 to prepare only Mac.
- Downloads are cached in `~/Library/Caches/Kauri/native` (override KAURI_NATIVE_CACHE); prepared inputs live in build/native-deps. Rerun preparation after SDK-lock changes or removal of those inputs. Linux SDKs are build inputs, not shipped libraries.
- IDE cinterop reads repository declaration headers, not prepared archives/SDKs. Final links track the SDK inputs. An editing machine does not need copied Mac link outputs for model import.
- Settings own repositories, including a restricted Binaryen Ivy endpoint. Strict configuration cache and resource limits remain enabled.
- Plugin tasks build production Wasm and Native code. The only extra transformations use standard Exec tasks to pack assets and generate target resource objects.
- Windows discovers an installed Edge WebView2 Runtime, loads an absolute runtime DLL with restricted dependency search and calls the accepted-risk five-argument internal entry. Modules remain cached for process lifetime; no handwritten C/C++ loader or adjacent loader DLL is shipped.
- Linux directly depends on GTK/WebKitGTK; missing libraries can fail before main.
- scripts/release-all-mac.sh links releases, runs non-GUI Native/resource tests, stages artifacts, strips/ad-hoc signs Mac and runs CLI/HTTP checks. GUI smoke requires an explicitly owned runner and is NOT RUN by default.
- scripts/release-mac.sh uses the same flow and canonical output path for Mac only. Release/smoke locks live under .gradle/kauri, outside clean outputs.
- See [build rules](checklist/build.md), [historical simplification](resources/minimal-static-host.md), [build review](resources/build-review.md) and [IDE import repair](resources/root-idea-sync.md).

## Run

```text
build/release/
├── macosArm64/Kauri
├── mingwX64/Kauri.exe
├── linuxX64/Kauri
├── linuxArm64/Kauri
├── SHA256SUMS
└── VALIDATION.txt
```

Copy the matching executable independently, then run it:

```bash
# macOS
~/ACodeSpace/demo/Kauri/build/release/macosArm64/Kauri
# Linux, after extracting the matching architecture
chmod +x Kauri && ./Kauri
```

On Windows, run Kauri.exe. Gradle, a JDK, Node.js and repository sources are not runtime requirements.

SHA256SUMS records integrity. VALIDATION.txt records checks against specific hashes, not universal runtime certification. Mac-only releases invalidate an old full-matrix manifest.

**Single-file delivery does not bundle the browser engine.**

| Target | System runtime requirements |
| --- | --- |
| macosArm64 | System WKWebView with WasmGC; build host was macOS 26.5.1 |
| mingwX64 | Windows 10/11 x64 and a modern WasmGC-capable Microsoft Edge WebView2 Runtime |
| linuxX64 | glibc x86-64, libgcc_s.so.1, libcrypt.so.1, GTK 3, WasmGC-capable WebKitGTK 4.1 and a graphical session |
| linuxArm64 | glibc aarch64 and the same Linux libraries/graphical environment |

- Debian/Ubuntu commonly package the Linux dependencies as libgtk-3-0 (or a t64 variant) and libwebkit2gtk-4.1-0. Older WebKitGTK versions may lack required Wasm features. Missing dependencies do not trigger an external-browser fallback.
- Mac/GTK use non-persistent browser contexts; Windows uses its own temporary UDF. Native notes remain persistent. Old browser profiles are not migrated or deleted.
- Mac ad-hoc signing is not Developer ID notarization. Windows artifacts are not Authenticode signed.

## Tests and evidence

```bash
# On the Mac builder, from the repository root
bash scripts/gradle-mac.sh :app:shared:macosArm64Test \
  :app:desktop:macosArm64Test :framework:macosArm64Test
bash scripts/test-build-policy.sh

# GUI commands open ordinary windows: only use an owned graphical runner.
KAURI_ISOLATED_DISPLAY=1 bash scripts/smoke-mac.sh build/release/macosArm64/Kauri
# On an owned Linux target runner only; not executed in the current iteration.
KAURI_ISOLATED_DISPLAY=1 bash scripts/smoke-linux.sh build/release/linuxX64/Kauri
```

- Native smoke copies only the executable into an empty temporary directory and exercises Compose Wasm, Native information/notes, Flow, RPC and actual native policy callbacks.
- GUI data is isolated, but KAURI_ISOLATED_DISPLAY=1 merely asserts runner ownership: it does not create isolation. Without a runner, do not run on the user's shared desktop or try to hide windows.
- Test-only frame-clock replacement was removed; all pages use the browser's frame clock.
- No owned GUI runner is currently available. GUI/context-isolation/Cocoa failure cleanup need fresh target execution; old build/smoke screenshots do not validate current artifacts. Compose UI tests do not prove native-host lifetime.
- [Current validation](resources/validation.md) separates links, CLI tests and GUI coverage. Historical evidence: [atomic rewrite](resources/atomic-rewrite.md), [offscreen removal](resources/offscreen-ablation.md), [fixed disposable contexts](resources/ephemeral-browser-context.md).
- `Kauri --licenses` prints embedded third-party notices without opening a window.

## Repository map

| Path | Responsibility |
| --- | --- |
| app/shared | Application @Rpc contract, DTOs and validation |
| app/desktop | Composition root, service/native business APIs and storage tests |
| app/frontend | Compose Wasm UI and generated kRPC client |
| framework | Atomic window/WebView, resource access and platform adaptation |
| gradle | Version catalog, toolchain/SDK locks and wrapper |
| scripts | Remote build/release and runner-only GUI smoke |
| checklist | Confirmed architecture and validation rules |
| kanban | Resumable Task Trees; Draft.md is user-owned |
| resources | Upstream findings, historical reviews and measured results |
| .agents/skills | Four context-workflow Git submodules |

Run `git submodule update --init --recursive` to restore workflow skills. No webview/webview runtime submodule remains.

## Experimental status

- Better developer experience than Tauri or smaller size than Electron/Compose Desktop are hypotheses, not measured conclusions.
- Omitting Chromium/JVM does not eliminate cost: Compose/Skia Wasm contributes to executable size, and system WebViews create helper processes.
- Automatic updates, installers, production signing/notarization and production-grade permission policies are not implemented. This is a feasibility experiment, not a mature Tauri replacement.
