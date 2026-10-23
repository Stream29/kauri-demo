# Task Tree

- `Inspect browser-context ownership and cleanup`()
- `Remove persistence option and fixed-profile path`()
- `Build four targets and run non-GUI regression`()
- `Verify artifacts and review contract`()
- `Update evidence and release owned resources`()

# Details

- User approved removing persistentBrowserData after distinguishing Native-owned business persistence from disposable browser state. Public window constructor becomes WebViewWindow(url, title), without replacement options.
- Always configure WKWebView non-persistent storage, GTK ephemeral context, and a fresh Windows temporary user-data directory. Remove the Windows executable-identity/persistent-directory helper and unused default GTK constructor binding; keep necessary partial-init, callback and temporary-directory cleanup.
- Preserve Native notes/authentication, typed kRPC, origin/navigation guards and streaming Source. Do not delete any old profile directories or user data.
- Windows still writes a UDF and needs bounded cleanup; temporary does not mean memory-only. No measured startup/RSS/I/O benefit is claimed.
- No owned graphical runner is available. Run no GUI on shared desktops; receipts must say NOT RUN. Four cross-links and Mac non-GUI tests cannot prove browser isolation or Windows cleanup at runtime.
- All builds on stream@macbook with scripts/gradle-mac.sh, detected/reused JVM and two workers. Initial remote relevant hashes match the completed offscreen-ablation baseline; no daemon initially present. IDEA MCP discovery returned no usable tools, so no forced import or IDE control.
- Preserve unrelated staged/untracked work; no stage/reset/commit. Only release task-owned resources.
- Checkpoint: four fresh release links, 14 forced non-GUI Native tests, resource 4/4, policy 12/12, Mac licenses/HTTP security/streaming and strict configuration-cache reuse passed. GUI was explicitly NOT RUN; no new browser isolation/cleanup proof.
- Current release/source manifests synchronized and independently SHA-256 checked locally. Source advisory review has no blockers; temp-path normalization suggestions were checked against documented GetTempPathW and private helper inputs, not added.
- Completed: final document review has no blockers; context and evidence updated. Only task-created daemon 22019 was stopped after verifying its expected JVM, user/version, IDLE state and last project. All GUI gates stay in the original native-host migration task; no workstation build/import, stage/reset/commit.
