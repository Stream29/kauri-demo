# Task Tree

- `Inspect window options and GUI test dependencies`()
- `Remove offscreen API and platform branches`()
- `Separate non-GUI release checks from isolated GUI checks`()
- `Rebuild four targets and run safe regression`()
- `Review evidence and release owned resources`()

# Details

- User confirmed removing the native offscreen feature after distinguishing Compose UI testing from native-host integration. Remove the parameter, platform positioning/activation branches and frontend frame-clock workaround; do not rename the option or add a replacement hidden mode.
- Keep persistentBrowserData and Source streaming unchanged. Keep document/new-window guards, thread checks and native lifetime handling.
- Ordinary Native unit tests must not create windows on a shared desktop. The existing Cocoa fault-injection test creates a real window: remove that offscreen-dependent unit case and explicitly record the resulting integration-test gap rather than count a no-op/skip as a pass.
- GUI smoke remains available only for an explicitly owned graphical runner, using ordinary visible windows. Default release checks must not launch it; record NOT RUN rather than reuse old screenshots. No owned graphical runner is available for this change.
- Compose's scene-based UI tests do not test WKWebView/WebView2/GTK lifecycle. This scoped ablation does not claim a new Compose/browser test suite or browser-only substitute for the application.
- Build only on stream@macbook with scripts/gradle-mac.sh, detected/reused JVM and two workers. Do not import/build with the user's IDEA or activate shared windows. Preserve unrelated staged/untracked changes; no stage/reset/commit.
- Remote relevant source hashes matched the current streaming baseline; no Gradle daemon was present initially. IDEA is running locally but MCP discovery exposed no usable project tools; no forced sync.
- Completed: four release links, 14 forced non-GUI Native tests, resource 4/4, policy 12/12, Mac licenses/HTTP security/streaming checks, strict configuration-cache reuse and format/dependency audits passed. Release explicitly reports GUI NOT RUN.
- Artifacts/source manifest synchronized and independently SHA-256 checked. Review dispositions and current hashes are in `resources/offscreen-ablation.md`; new native GUI gates remain in the original migration task, not claimed complete.
- Rechecked user/JVM/version, IDLE state and last project before stopping only task-owned daemon 14515. No SDK temporary directory or task lock remains. No workstation Gradle/import or GUI, no stage/reset/commit.
