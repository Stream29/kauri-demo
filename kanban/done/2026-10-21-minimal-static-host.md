# Task Tree

- `Update confirmed architecture and execution context`()
- `Replace custom platform hosts with the pinned static webview C API`()
- `Replace convention plugins with ordinary KMP configuration`()
- `Build four standalone executables on the Mac and test offscreen`()
- `Review evidence, document remaining coverage, and clean owned resources`()

# Details

- User authorized converging to the discussed minimal design, context first.
- Keep `app/{shared,frontend,desktop}` and `framework`, typed kRPC, embedded Wasm assets, and all four Native targets.
- Use webview 0.12.0 statically as the common window/WebView host. Do not maintain a second native router or native popup product.
- Preserve same-origin document restrictions and deny new windows. Ordinary execution uses a single browser-data context; smoke uses isolated temporary browser/business data.
- Prefer ordinary module `build.gradle.kts`, KGP-generated compile/link/distribution tasks, standard cinterop/static libraries/linker options. Remove the convention-plugin layer and bespoke staging/checksum task types.
- Prepare pinned native archives/SDK inputs separately from everyday Gradle builds; document bootstrap rather than hiding undeclared downloads in link tasks.
- Keep only necessary resource embedding task wiring; use standard tasks for file operations and release scripts for signing/checksums.
- Build on Mac only through the daemon-reuse helper with two workers. GUI checks must not activate user windows; Windows/Linux runtime coverage must remain explicit.
- Existing spike archives/logs are evidence and a recovery source, not production artifacts to relabel.
- No dependency upgrades, commit, or unrelated cleanup.

## Results

- Removed `build-logic` sources and old Windows/Linux host/loader adapters. Ordinary module scripts now declare KMP targets, cinterop/static archives and executable links; only standard `Exec` tasks pack/embed frontend bytes.
- Static webview archives are prepared by `scripts/prepare-native-mac.sh`, outside application Gradle builds. Linux SDK package paths/hashes are pinned in `gradle/linux-sdk.lock`; the upstream source is copied/patched under ignored build outputs, not edited in its reference submodule.
- One KMP `apply false` in root is retained after a real SwiftPM build-service classloader conflict when modules independently loaded KGP with different plugin sets. It shares the plugin classpath, not a custom plugin layer.
- Four full application release targets linked. Fixed cinterop callback string/Unit types, Win32 unsigned flags and MinGW's missing UUID template specializations by using official SDK IID constants.
- Native compiler dependency checks prepare the toolchains. Linux SDK search paths use final binary linkerOpts, because the cinterop command-line addition did not propagate into its manifest; final links declare the SDK directory input.
- Clean executable/embedded-output recovery with build cache disabled ran all four release links. Final configuration cache reuse: 454ms, 88 tasks, 6 executed / 82 up-to-date; not a cold-build benchmark.
- Six Native, four Node resource, eleven script-policy checks and Mac standalone Wasm/kRPC/Canvas/native boundary/seven backend checks passed. Native tests do not require frontend embedding.
- Mac smoke now asserts a nonpersistent WebKit data store. All targets share test-only result binding; business requests remain typed kRPC.
- PE imports/ELF dependencies audited after local SHA verification. Linux x64 `--licenses` passed without a display and with immediate symbol binding. Windows/Linux GUI and Linux arm64 execution remain unverified.
- agy Medium attempts timed out empty. Flash Low independently reviewed the build scripts and withdrew its initial three findings after facts/experiments; later FileTree/duplicate-flag claims were checked against code and live collection semantics, not blindly adopted.
- Native archive mutation probe forced a final link through the embedded klib input; inputs were restored and delivery rebuilt. Artifacts/hashes/smoke/XML are synchronized locally.
- Current implementation/report: `resources/minimal-static-host.md`, `resources/validation.md`. Previous validation moved to `validation-before-static-host.md`.
- Archived the legacy build-logic XML; removed unused build-logic directory, old host/SDK outputs and obsolete cinterop outputs. Prepared native inputs/download caches remain intentionally reusable.
- Native-input probe scratch was restored/removed. Final document/code checks passed; upstream submodule unchanged. Stopped only the task-owned idle Gradle daemon 22818 after PID/JVM/last-project/idle checks; released the lock.
- No Git commit or unrelated process/window control. IDE MCP was disconnected, so no claim of an actual IDEA sync.
