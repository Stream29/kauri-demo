# Task Tree

- `Read actual IDEA sync failures and identify required inputs`()
- `Remove root lifecycle aggregation and update callers`()
- `Separate header binding generation from final native linking`()
- `Validate remote builds and confirm IDE import without taking over user resources`()
- `Update evidence and release owned resources`()

# Details

- User requested root cleanup and reported a failing IDEA Gradle sync.
- Keep only the KMP classpath `apply false` declaration in root; no `base`, root `check`, or root `assemble` wrappers.
- Actual local IDEA log and daemon 1167702 show `CInteropProcess` validation failures: `build/native-deps/{linuxX64,linuxArm64,mingwX64}/{libwebview.a,SHA256SUMS}` are absent on the editing machine.
- Root cause is coupling IDE binding/commonization to Mac-only prepared link artifacts, not a proven IDE cache defect. Do not copy whole cross SDKs or skip IDE checks to hide it.
- Generate bindings from repository public C headers only. Declare prepared static archives and system SDK inputs on final Native binaries, including tests; production links must still require the real archives.
- Compile application/WebView on Mac through daemon-reuse helper. Do not launch local application builds, stop user daemons 497519/1167702, reset IDE caches, restart/focus windows, or modify unrelated projects.
- IDE metadata/binding refresh may need separate user permission if it would trigger local compiler tasks; inspect existing IDE state/logs first. No new root wrappers or IDE-only build variants.
- No dependency upgrades or Git commit. Preserve pre-existing staged changes.

## Checkpoint

- Root is now only the three-line KMP classpath declaration. Release script directly selects four module link tasks and three Mac test tasks.
- Removed `staticLibraries`, libraryPath and archive/SHA task inputs from cinterop. It reads public headers from the reference source, not generated SDK headers. `.def` records `-lwebview`; binary linker search paths and explicit archive inputs perform real static linking.
- On Mac, moved the entire `build/native-deps` directory away and forced `:framework:prepareKotlinIdeaImport --rerun-tasks`: passed. Inputs restored by trap. No stub archive, skip flag or IDE-specific model variant.
- Remote release/reuse/policy validation passed; final header fix also passed all four releases, Mac smoke/security and native/resource tests. Final reuse: 621ms, 88 tasks, 6 executed / 82 up-to-date, not a cold benchmark.
- User approved local IDEA model/cinterop generation only. Used existing daemon 1167702/JDK 26 with two workers; did not build app/webview locally or stop user daemons.
- Actual Kauri imports RESOLVE_PROJECT:4/7 succeeded and finished importing; other-project task IDs were excluded. Module list recovered from empty to 76 project/source-set entries.
- Found and fixed shared-code `UnsafeNumber` diagnostic by changing the bounded asset size return type from `unsigned long` to `uint32_t`, not adding a suppression. Header changes regenerated cinterop/commonized bindings normally.
- IDEA diagnostics are clear for root/framework/desktop/frontend scripts, shared native host and asset reader. No claim of whole-project lint.
- Latest artifacts synced and hash-checked, Linux x64 headless immediate-bind CLI passed; current PE/ELF audits preserved.
- Curated IDE success evidence uses only Kauri task IDs 4/7, not other project success events. Input-directory move restored, three task-created temporary inspection scripts removed.
- Only task-owned idle Mac daemon 36330 stopped after JVM/PID/last-project/idle checks. Local user daemons and IDE untouched; prepared native inputs and IDE binding caches retained.
- Context and results updated in `resources/root-idea-sync.md` / `validation.md`; final consistency check and sync completed. No Git commit.
