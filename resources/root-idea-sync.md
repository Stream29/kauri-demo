# Root build cleanup and IDEA import repair

Date: 2026-10-21. User request: remove redundant root build content and investigate the actual IDEA Gradle sync failure.

## Root build

- build.gradle.kts retained only three lines sharing the KMP classpath with apply false. No base, root check or root assemble.
- Release scripts select existing four module release-link tasks and three Mac test tasks, not a new plugin, extension or lifecycle wrapper.
- The shared KMP classpath remained because an earlier actual experiment reproduced a build-service classloader conflict across differing module plugin sets.

## Reproduced failure and fix

- Local IDEA/daemon 1167702 logs showed CInteropProcess input validation failing because build/native-deps/{linuxX64,linuxArm64,mingwX64}/{libwebview.a,SHA256SUMS} did not exist. This concerned the historical wrapper baseline.
- Mac-prepared link products should not be prerequisites for editing-machine bindings. The fix did not delete global caches, copy SDKs or bypass cinterop.
- Bindings use repository public headers with KGP's own toolchain. Removed staticLibraries, libraryPath and archive/SHA binding inputs; the historical .def retained -lwebview/system link options.
- The historical wrapper repair tracked archives through standard linkerOpts. Current direct hosts need only Linux link SDKs and platform libraries/frameworks; IDE bindings read repository declarations.
- On Mac, temporarily moved all build/native-deps aside and forced :framework:prepareKotlinIdeaImport --rerun-tasks: passed. A trap restored the directory; no IDE-only build branch was introduced.

## Commonization diagnostic after import

- IDEA then diagnosed platform-width disagreement in EmbeddedAssets.kt: C unsigned long is 32-bit on Windows and 64-bit on Linux/macOS.
- Changed the interface to uint32_t. The archive was already limited below 2 GiB, so fixed width matched the format/runtime constraints. No UnsafeNumber suppression.
- Normal import preparation detected the header change, regenerated/commonized bindings, without custom generation tasks or disabling commonization.

## Actual IDEA verification

- The user explicitly allowed only local model import/necessary cinterop generation; app/webview compilation stayed on Mac.
- Refreshed Kauri in the background through IDEA APIs without activating tool windows, navigating errors, restarting IDE or altering global JVM/cache settings.
- Explicitly reused detected local daemon 1167702/JDK 26, max two workers. The user's daemon was not stopped.
- IDEA logs matched Kauri's path and RESOLVE_PROJECT:4/7 onSuccess/onImportFinished; other projects' import events were not counted.
- MCP module entries recovered from zero to 76 project/source-set entries, including four Gradle modules and Native/Wasm sets. These are IDE source-set representations, not 76 new business modules or a JVM backend.
- Inspected root/framework/desktop/frontend scripts, shared host and resource-access files had no IDEA ERROR diagnostics; the width issue disappeared. No claim of exhaustive whole-project static analysis.

## Remote regression and evidence

- Four release targets, Native 6, resource 4, policy 11, Mac single-file offscreen Wasm/kRPC/Canvas/Flow/native guards and seven backend security checks passed.
- Repeat reused configuration cache. [validation.md](validation.md) records sizes/hashes/coverage; Windows/Linux GUI coverage did not expand.
- build/review/root-sync-{idea-before,import-no-native-inputs,fixed-width-import,final-release,final-reuse,policy}.log holds failures, missing-input checks, binding regeneration and release evidence; scoped IDEA success events were archived separately.
- Removed three temporary tools, restored inputs and stopped only task-owned idle Mac daemon 36330. Local IDE/user daemon and prepared/download/binding caches remained. No commit was created in that iteration.

## Official basis

- [Kotlin definition files](https://kotlinlang.org/docs/native-definition-file.html): binding headers, linkerOpts and optional staticLibraries have distinct responsibilities.
- [KMP Native/cinterop DSL](https://kotlinlang.org/docs/multiplatform/multiplatform-dsl-reference.html): use standard cinterop and binary linking rather than another lifecycle task.
