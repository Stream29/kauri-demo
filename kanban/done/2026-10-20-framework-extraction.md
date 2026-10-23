# Task Tree

- `Inspect module responsibilities and build dependencies`()
- `Extract framework and relocate application modules`()
- `Update packaging scripts and architecture context`()
- `try {`
  - `Build all four release targets on remote Mac`()
  - `Run Native tests, offscreen WebKit smoke, and security checks`()
  - `Record measured artifacts and remaining runner gaps`()
- `} finally {`
  - `Clean owned temporary resources`()
- `}`
- `Review changes and hand off`()

# Details

- Request: move the existing application modules under `app/` and extract reusable infrastructure into `framework/`.
- Application: `app/shared` owns the demo RPC contract; `app/frontend` owns Compose/Wasm UI; `app/desktop` owns the executable entry point, note service, and native business APIs.
- Framework: native window/WebView lifecycle, embedded assets, secure loopback serving, launch configuration, and reusable packaging support. It must not depend on application contracts or implementations.
- Preserve typed kRPC, four Native targets, single-file delivery, navigation restrictions, and offscreen testing. Do not mix in a platform-host language rewrite.
- All Gradle/native compilation runs on `stream@macbook`; reuse the detected daemon JVM with `scripts/gradle-mac.sh`.
- IDE MCP connection was unavailable during inspection; filesystem tools are used without controlling IDE windows.
- Initial prototype evidence remains historical; record new validation separately rather than reusing old results as proof of the refactor.
- Static checks: no framework imports of demo contracts or implementations; shell syntax and whitespace checks passed. Application and framework tests now have separate owners.
- Completed: all four release targets rebuilt; 6 Native tests passed (2 application contract, 1 persistence, 3 framework); Mac standalone offscreen Canvas/kRPC/Flow and 7 HTTP/security checks passed.
- Framework test task graph contains no application compile/link tasks. Global Kotlin SwiftPM lock-metadata tasks remain; these are not application code dependencies.
- Updated npm workspace names and removed obsolete generated workspace/lock entries. Normal release verification passed lock checks.
- Added explicit resource/host object inputs to application link tasks. Final incremental matrix verification: 1s, 9 executed tasks and 85 up-to-date; not a cold-build measurement.
- Updated artifacts and smoke evidence are local under `build/`; all four SHA-256 checks passed. Windows imports and both Linux dependency tables were re-audited; linuxX64 `--licenses` exited 0 without GUI.
- Windows/Linux GUI and Linux arm64 execution remain unverified. Exact sizes and validation coverage are in `resources/validation.md`.
- Removed obsolete remote module copies only after verifying their original source hashes. Temporary smoke resources were cleaned; only this task's idle Gradle daemon (71096) was stopped.
- README, architecture, and validation records were reviewed for redundancy, boundary consistency, and measured-vs-inferred claims. No Git commit was requested or created.
