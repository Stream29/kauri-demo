# Fixed disposable browser context

Status: 2026-10-22, implemented and rebuilt. Non-GUI checks passed; browser-context isolation and cleanup still need target GUI runners.

## Scope

- Public window API is now `WebViewWindow(url, title)` with run/requestClose. Removed persistentBrowserData without a replacement option, profile object or configuration layer.
- macOS always sets WKWebsiteDataStore.nonPersistentDataStore before creating WKWebView.
- Linux always creates an ephemeral WebKitWebContext and passes it to the WebView. Removed the unused default-context constructor declaration/fallback.
- Windows always creates a unique temporary UDF. Removed executable-path identity hashing, persistent directory creation and the framework's unused shell32 linker option. The demo still uses shell32 for its Native business-data directory.
- Keep partial-initialization, COM late-callback/module lifetime, temporary-directory ownership and bounded cleanup. Temporary does not mean memory-only or guaranteed deletion after a crash.
- Ordinary Native note persistence, authentication, origin/navigation checks, typed kRPC and streaming resources are unchanged. Existing browser profiles are neither migrated nor deleted.

## Validation boundary

- The change removes API/behavior branches; startup, RSS and disk-I/O improvement has not been measured. Losing cross-launch browser state can also lose cache benefits.
- No owned graphical runner is available. GUI, browser-context isolation and platform cleanup must be verified on target runners; compile/link and non-GUI HTTP tests cannot prove those behaviors.
- Previous delivery is [offscreen ablation](offscreen-ablation.md). Its hashes and receipt are historical after this rebuild.

## Platform evidence

- [Apple non-persistent store](https://developer.apple.com/documentation/webkit/wkwebsitedatastore/nonpersistent()) is the configured store, not a custom profile manager.
- [WebKitGTK ephemeral context](https://webkitgtk.org/reference/webkit2gtk/stable/ctor.WebContext.new_ephemeral.html) provides ephemeral WebViews without persistent client storage. The linked API is 4.1; target runtime compatibility still requires execution.
- [WebView2 UDF lifecycle](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/user-data-folder) explains disk-backed browser data, writable UDF requirements and ending browser sessions before directory deletion. The host keeps its existing bounded cleanup policy and reports failure instead of claiming successful removal.
- Official references were checked in the preceding discussion on 2026-10-22; this change does not upgrade SDKs/dependencies or introduce a performance guarantee.

## Verification

- Four release links actually executed on the remote Mac: 1m 13s, 91 tasks (26 executed / 65 up-to-date). Gradle 9.6.1 used task-owned daemon 22019, its detected GraalVM JDK 21.0.8 and two workers. No workstation Gradle/import.
- Same qualified task group reused strict configuration cache in 614ms, with 6 executed / 85 up-to-date and four executable links up-to-date. These are build observations, not measured application performance benefits.
- Forced Native rerun: 14 tests (framework 8, desktop 4, shared 2), no failures/skips. Node archive tests 4/4 and script-policy checks 12/12 passed.
- Mac licenses CLI and seven HTTP security checks passed. Interrupted slow resource download was followed by a fresh chunked response matching all 8,652,729 Wasm bytes. The no-length writer backpressure test again stopped at 1 MiB of its 8 MiB fixture and closed on cancellation.
- Release explicitly used `KAURI_ISOLATED_DISPLAY=0`; GUI is NOT RUN in the receipt. Native browser creation/store selection is covered by source inspection and four-target linking, not runtime isolation testing. Windows/Linux are compile/link/format/dependency audit only, not CLI/GUI.
- Format/dependency audit confirms Mach-O arm64, PE x86-64, ELF x64/arm64. Existing system WebView dependencies remain; final Windows executable still imports shell32 for Native application directories.
- Evidence is in `build/review/ephemeral-browser-context/` (logs, test XML, source manifest); checksums and coverage receipt are in `build/release/`. No old screenshot is treated as current GUI evidence.
- Release binaries and the complete source/build-script manifest were synchronized and independently SHA-256 checked locally. Old release checksums/receipt were preserved in `build/review/remove-offscreen/`. Only task-owned idle daemon 22019 was stopped after ownership/JVM/state verification; no lock, temporary SDK, GUI or workstation build/import, and no Git commit. Completed task: `kanban/done/2026-10-22-ephemeral-browser-context.md`.

| Target | Release bytes | SHA-256 |
| --- | ---: | --- |
| macosArm64 | 17,043,248 | `7b52106cdc406279ad2973706834b81b0d3564721f315bba763fea88be7c0880` |
| mingwX64 | 19,701,760 | `3325d69cc1ef06cb4d30073f09d81d8201e918fc040c56d46310821bb0cef8c5` |
| linuxX64 | 18,801,552 | `5551763405f541dfaa3051ce413dae73c58768a3962b3c0b36becd77239ca6b7` |
| linuxArm64 | 18,270,232 | `6362624d406dd6d6ecd91decff6d78c811962af2a699a8d71ba930408fd2eb05` |

## Review disposition

- agy Gemini 3.8 Flash Low reviewed the public API, changed platform paths, retained cleanup and contract; no blockers. Its suggested path-delimiter normalization was not added.
- [GetTempPathW's documented result](https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-gettemppathw) ends with a backslash, and the existing checked return range rejects failure/truncation. The private helper appends a UUID directory name without a trailing slash; recursive cleanup also appends names without trailing slashes. The review's alternate delimiter inputs are not present in this path.
- The unchanged callback/lifetime and cleanup risks remain explicit runtime gates; advisory review is not proof of Windows deletion or pure-memory storage.
- Final document review found no blockers or unsupported runtime/performance claims; raw source/document responses are archived under `build/review/ephemeral-browser-context/`.
