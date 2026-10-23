# Task Tree：direct Kotlin/Native system host

- `Record confirmed direct-native target and accepted loader risk`()
- `Delegate migration to Session 1029`()
- `Implement first-pass hosts and run available release checks`()
- `Repair acceptance blockers from parent review`()
- `Rebuild releases and rerun targeted regression checks`()
- `Update evidence and architecture; release owned resources`()

- **`Await isolated Windows/Linux runtime gates and user acceptance`()**
- `if (runtimeGatesPass() && userAccepts()) {`
  - `Move this task to done`()
- `}`

## First-pass checkpoints (historical)

- Read `AGENTS.md`, planning/kanban/document/code-abstract/IDE/remote-Mac/Gradle skills, `checklist/architecture.md`, `checklist/build.md`, `resources/pure-native-feasibility.md`, `resources/minimal-static-host.md` and `resources/root-idea-sync.md`.
- Delegation was accepted and implemented in [Session 1029](file:///home/stream/.kodex/sessions/1029). That subagent's separate agy review attempt failed with `RESOURCE_EXHAUSTED`; it does not negate the ordinary subagent implementation.
- Compared local and `stream@macbook` file checksums before editing; used selective `scp`, never a blanket `rsync --delete`. Existing staged/untracked work was not reset, staged, committed or overwritten.
- Implemented direct Kotlin/Native hosts:
  - macOS AppKit/WKWebView with offscreen non-persistent smoke profile and navigation/new-window delegates; orderly close was repaired in Session 1034.
  - Windows Win32/WebView2 official SDK cinterop, trusted absolute runtime discovery, accepted internal entry, real per-IID COM `QueryInterface`/`AddRef`/`Release`, StableRef retention, callback exception guards and HMODULE lifetime.
  - Linux GTK/WebKitGTK declaration-only cinterop with ephemeral smoke context, policy/create/delete callbacks and StableRef cleanup.
- Deleted wrapper implementation/build boundary: `framework/src/nativeInterop/webview.patch`, `webview_policy.cc`, old wrapper cinterop header/def, static archive preparation and `resources/webview` submodule. Added official WebView2 SDK declarations and Linux declaration header.
- `scripts/gradle-mac.sh` reused daemon PID `45062`, GraalVM JDK 21.0.8 and two workers. `prepare-native-mac.sh all` succeeded without compiling C/C++ host code.
- Four release links succeeded with strict configuration cache. The first Windows link exposed missing final-app system linker options; adding Win32/COM system libraries to `app/desktop` fixed it. The final `release-all-mac.sh all` passed resource tests, Native tests, Mac smoke and security smoke.
- Mac runtime evidence: Compose Canvas 110 colors; Native systemInfo; Unicode note; oversize rejection; Flow 1/2/3; unauthorized RPC 403; foreign navigation/new-window rejection; `policyVerified=true`; HTTP security 200/403/404 matrix; close cleanup.
- Windows/Linux links were cross-compile/file-format coverage only, not execution of CLI or GUI. No isolated Windows or Linux GUI runner was available.
- Artifacts synced to `build/release/`; hashes and sizes are recorded in `resources/validation.md` and `build/release/VALIDATION.txt`.

## Session 1034 repair checkpoint

- Required checklists/review and platform sources read; local/remote hashes compared before selective sync. Remote daemon 45062/JDK 21.0.8 reused with two workers. Local IDEA module metadata read without import/build; historical index/untracked work untouched by Git operations.
- Fixed conditional smoke state, application title, five-argument ABI/argument forwarding, COM ownership/tokens/late completion/failure cleanup/module safety/resize, GTK policy/evaluate/message bridge and AppKit/Win32/GTK orderly close. No architecture reselection.
- Four updated single-file releases linked on Mac; current artifact table/hash/receipt in `resources/validation.md` and `build/release/`. Fresh evidence under `build/review/direct-native-repair/`; first-pass manifest/receipt copies preserved separately.
- Passed: Mac offscreen no-smoke/no-result-dir ordinary initialization/delegate-close/finally; real Canvas/kRPC/Flow/policy/security smoke; 8 Native tests; resource 4/4; build-policy 11/11; strict configuration-cache reuse/unchanged links; missing embedded-object producer recovery with identical bytes. Windows-x64 compiler ABI probe and Mac C-function-pointer forwarding are contract evidence, not Windows execution.
- `resources/direct-native-review.md` now records a conclusion for each original finding. Source blockers are repaired; Windows/Linux target runtime gates and human acceptance remain pending.
- Parent reviewed the targeted source fixes and fresh logs, independently checked all ten source-manifest entries and four executable hashes/formats/sizes, and accepted the repair delivery within that scope. No additional build or GUI run was performed by the parent; Windows/Linux runtime gates remain open.
- Subsequent deletion-oriented ablation is complete in `kanban/done/2026-10-21-native-host-ablation.md`: 97 host Kotlin lines removed, fresh four-target links and Mac regression gates passed, current artifacts updated. It does not close the Windows/Linux runtime gates below.
- Public integration was subsequently reduced to one typed `runDesktopApplication` function; `DesktopApplication` and external server/Route callbacks are removed. See `kanban/done/2026-10-21-public-api-ablation.md`; its fresh release/Mac regression evidence and current hashes are in `resources/validation.md`. Target-runtime gates below remain unchanged.

## Remaining risks / next resumable work

- After the approved offscreen ablation (`resources/offscreen-ablation.md`), Mac also needs a fresh owned-runner GUI pass: normal startup/close, Canvas/kRPC/Flow, actual native policy callbacks and Cocoa failure cleanup. Current release runs no GUI by default; earlier offscreen evidence cannot close this new gate.
- After fixed disposable browser contexts (`resources/ephemeral-browser-context.md`), verify no cross-launch browser-state carryover on Mac/Windows/Linux, orderly temporary-context/UDF cleanup, initialization-failure handling and Native note persistence across normal app restarts. No GUI runner was used for this API reduction; do not treat non-GUI tests as context-lifetime proof.
- The previous host source is superseded by the user-approved atomic rewrite in `kanban/done/2026-10-22-atomic-framework-api.md` / `resources/atomic-rewrite.md`; keep first-pass and repair evidence historical, not current implementation proof.
- Await owned isolated runners. Windows: installed WebView2/internal entry, actual COM/token/async-close behavior, resize, ordinary close and full Wasm/kRPC/Flow/smoke/security. Linux x64/arm64: policy/response loading, WasmGC, real painting and ordinary/smoke shutdown. There is no longer a native JS evaluate/message channel to validate; test completion is app-owned HTTP.
- The task deliberately stays executable until those actual runtime gates and user acceptance are complete. Do not relabel first-pass hashes/smoke or compile-only probes as target runtime evidence.
- Windows `CreateWebViewEnvironmentWithOptionsInternal` is intentionally retained and may change across WebView2 Runtime versions; the user accepted this risk. Validate on an isolated Windows 10/11 runner with installed WebView2 Runtime.
- Do not restore `webview/webview`, C/C++ wrapper code, Rust, official loader DLL, root aggregate tasks, or prepared-archive cinterop inputs without a new architectural decision.
