# Build governance and independent review

Initial governance: 2026-10-20; simplification: 2026-10-21. Scope was builds, not host rewrite/dependency upgrades.

Historical record. Later authorized static-host migration removed conventions; see [historical convergence](minimal-static-host.md) and [current validation](validation.md).

## 2026-10-21 task-necessity review

- Gemini 3.8 Flash Medium completed an independent source review and factual follow-up. Initial input contained full tasks/registration/tests/archive reader/release scripts without the main assistant's prior categorization; counterevidence/bootstrap context was supplied afterward.
- Earlier Opus/Argon/Sonnet attempts returned quota 429; after the user's reminder Flash succeeded. Failed requests were not reviews.
- Raw ignored responses: build/review/agy-task-necessity-flash.json and agy-task-necessity-flash-followup.json. No builds/implementation changed during that review.
- Verified inventory: **6 custom types, 18 custom instances, 5 extra aggregate tasks**. Expanding four target instances is not itself overdesign.

| Type | Verified finding |
| --- | --- |
| PackAssets | Private runtime archive justified a transform. Zip was not a drop-in without changing runtime, nor inherently dependent on a particular C library |
| EmbedAssets | Single-file delivery needs embedding. Custom class or Exec can orchestrate it; splitting assembly generation/compilation may not reduce total code |
| CompileNativeHost | Encapsulation had value, especially Windows' two compiler calls. Consider narrower toolchain initialization, not blindly deleting prerequisites |
| StageExecutable | Separate non-Mac copy/mode from Mac strip/sign if desired, preserving nonoverlap/signing/failure validity |
| ReleaseChecksums | Review one global/four local manifest policy first. Generation differs from verification; Mac-only consumers must change with it |
| ExtractWebView2Headers | Standard archive/Sync could extract, but validation, required headers, obsolete outputs and cache behavior must be explicit |

### Priorities and corrections

- Priority 1: five checksum instances in historical build-logic/src/main/kotlin/kauri.desktop.gradle.kts#L98-L107. A consolidated manifest must describe this invocation's target set, not leftover files. Mac-only global removal prevents staleness, not a Gradle validation error.
- Priority 2: manual ZIP traversal in build-logic/src/main/kotlin/dev/kauri/gradle/SdkTasks.kt#L31-L72 could use standard facilities. No verification-metadata.xml existed; Gradle did not automatically replace SDK SHA checks, and Sync did not preserve FROM-CACHE automatically.
- Priority 3: StageExecutable in build-logic/src/main/kotlin/dev/kauri/gradle/ReleaseTasks.kt#L23-L67 could narrow platform responsibility. Disabled build cache does not mean no up-to-date checking or a redundant class.
- Flash withdrew the eager providers.exec allegation: construction was lazy, with no configuration-time get(); :help had not executed xcrun.
- Flash withdrew unconditional removal of main-compilation dependency: it also initialized LLVM/sysroot. Establish a narrower readiness point and test isolated cold tools first; never delete shared .konan/dependencies.
- Corrected arithmetic: replacing only SDK/checksum while retaining Stage yields **4 types, 12 custom instances**, not three types or twelve total tasks. Standard replacements still exist. This was unimplemented.
- “Read pointers directly” was not zero-copy: runtime readBytes allocated ByteArray. No evidence that more parallelism/fewer tasks was faster.

### Official basis

- [ProviderFactory.exec](https://docs.gradle.org/current/dsl/org.gradle.api.provider.ProviderFactory.html) output is lazy; configuration-time reading makes it a model input.
- [Sync](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/Sync.html) is not build-cache enabled by default and cleans destination. [Dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html) needs explicit trust/metadata.
- [Incremental builds](https://docs.gradle.org/current/userguide/incremental_build.html) permit runtime input/output declarations and recommend typed tasks for nontrivial implementations; no blanket ban on custom classes.
- Web docs were newer 9.x; pinned Gradle remained 9.6.1. Replacement DSL/cold/cache/partial-full release behavior still required validation.

## 2026-10-21 simplification

- User challenged complexity/root apply false. Removed five root plugin aliases and checkNative forwarding; apply false is not a universal multimodule requirement.
- Replaced five handwritten plugins with four precompiled convention scripts, removed plugin-ID/class registration and returned frontend-only Wasm/publication to app/frontend.
- Removed single-use DesktopPackaging extension; releases used build/release. Catalog provided versions; build-logic's KGP supplied convention plugin classpath.
- Used native dependencyScope/resolvable/consumable roles instead of manual flags. Output providers propagated custom producers; KGP's known raw link provider still needed explicit dependency.
- Retained real archive/embedding/C++/SDK validation/signing transforms, not plugin classes merely for reuse.

### agy decisions

- Claude Sonnet 5.5 Medium read-only review: ignored build/review/agy-simplify.json.
- Accepted precompiled conventions, frontend-local config, removing single-use extension and native roles.
- Rejected “no tests, remove JUnit”: omitted review input did not erase 14 real tests.
- Retained shared target convention (three consumers) and root/Wasm web-tooling scope.
- A fixed JDK alone cannot guarantee daemon reuse; actual JVM arguments matter and detection/reuse was required.
- Copy/doLast were not universal substitutes for successful-sign-before-publish or SDK verification. Global dependency verification needs its own trust policy.
- Clarified the frontend explicit builtBy exception. Other alleged issues were not reproduced: lazy xcrun did not break Linux IDE import, same-named cross-project configurations are valid, KGP used the same catalog, Native test binaries are not Executable, and tests did not pack production UI.

### Verification

- Initial remote :help/:check passed with root apply-false list removed; conventions/plugin validation/build-logic 14/Native 6 passed.
- Deleted application binaries/assets/host objects and observed four real links/three C++ producers; Mac standalone offscreen Canvas/kRPC/Flow/security seven passed.
- Repeat configuration reused cache: 108 tasks, 6 executed/102 up-to-date; policy 11 passed.
- Deleted pack/SDK headers restored offline FROM-CACHE with unchanged four hashes.
- User-added IDEA investigation: local logs showed intermediate 10:04 script errors (java.io.File shadowing, file opt-in placement), then successful import at 10:07:24 after correction. Only existing logs were read; no local build/window control.

## Original independent review method

- Claude Sonnet 5.5 Medium initial/source follow-up, without tools/edits/tests. Raw responses stayed in ignored build/review; raw execution logs/conversation identifiers were not published by that review.
- Findings below distinguish reproduced defects from speculation; static review is not runtime validation.

| ID | Topic | Disposition |
| --- | --- | --- |
| A01 | Configuration paths, task-name coupling, frontend in tests | Typed plugins/declarative artifacts/producers; get() of a path alone is not dependency resolution. UI resources linked only to app executables |
| A02 | Missing native compile inputs | Declared LLVM/sysroot/source/SDK/upstream header/target inputs and real object link inputs |
| A03 | Nondeterministic LLVM glob | Pinned Kotlin 2.4.0 distribution; checked konan.properties, llvm-21-aarch64-macos-essentials-97 and three sysroots |
| A04 | SDK markers/download/extract | Restricted Gradle NuGet/Ivy resolution; hash/path/obsolete-output checks. Concurrent download was not a demonstrated failure of the serial single-Windows producer |
| A05 | Daemon/JVM/post-hoc checks | [Ownership rules](../checklist/build.md#build-ownership), identity in cache validation, no guessing on parse failure |
| A06 | Incompatible bootstrap | Explicit-JVM minimal temp build; fresh PID/reuse guard/cache actually checked |
| A07 | Stale release/manifest mistaken for certification | Typed sign/check, replace after validation; SHA is integrity, separate successful VALIDATION.txt states runtime gaps. No basis for arbitrary strip/memory changes |
| A08 | Diverging release entries | Mac forwards to canonical Mac path/full pipeline; partial releases invalidate full manifests |
| A09 | Failed smoke logs/unbounded TERM | Preserve exit/logs; terminate only owned child, bounded TERM then KILL; isolated result-directory fixture |
| A10 | Scattered versions/API uncertainty | Catalog/toolchain description without upgrades; verify Node/Yarn/Wasm/-ldl against builds/source, not uncertain review wording |

## Additional discoveries

- KGP raw Provider<File> lost producer information through adaptation; staging could reuse stale files. Explicit typed link dependencies and actual clean-output links, not BUILD SUCCESSFUL alone, were required.
- Published an explicit host object file instead of scanning nonexistent outputs while saving task graphs.
- FAIL_ON_PROJECT_REPOS conflicted with KGP automatic tooling repositories. Supported EnvSpec.downloadBaseUrl absence retained Binaryen through restricted settings Ivy, without relaxing policy/disabling optimization.
- :check and unqualified check have different selection scopes. Scripts use qualified tasks; helper rejects common ambiguous selectors.
- Locks/owned-daemon marker moved from clean-deletable build to .gradle/kauri and span release/offscreen checks.
- Removed deprecated Compose dependency shorthand, retaining Material3 1.9.0 rather than incorrectly changing it to UI 1.11.1.

## Follow-up dispositions

Review severity labels were not treated as evidence.

| ID | Disposition |
| --- | --- |
| R01 Arguments/identity | Reject diagnostics/external init/project-settings routing. Existing pipefail/nonzero handling and missing-observation failure contradicted the claimed bypass. --no-configuration-cache still validates identity |
| R02 Lock adoption | Actual parent PID + environment owner + owner file, plus parent liveness/stale-lock recovery advice. Cooperative protocol, not a sandbox against malicious same-user code; stale/unrelated owners tested |
| R03 Failures/interruption | Clean only newly created partial owner state; never interrupt user builds or stop unknown daemons. Tests use owned children |
| R04 strip resolution | Do not restore unresolved dispatchers: providers.exec is lazy; :help --no-configuration-cache did not call xcrun --find strip. Tool-change cache invalidation is expected |
| R05 Target matrix | Exact manifest directory-set/missing-target tests. Full assembly updates target/global checksums after Mac-only transitions |
| R06 Documents/evidence | Final reports replace provisional counts; Linux display consent is implemented/refusal-tested. Rules and review history avoid duplicate ownership procedures |

## Final historical verification

| Check | Result |
| --- | --- |
| Build logic | 14 JVM tests and validatePlugins passed; warnings treated as errors, no TestKit competing daemon |
| App/framework | Native 6: contract 2, storage 1, framework 3; tests exclude production UI pack |
| Policy | 11 cases covering worker/JVM/selector/diagnostic/routing/init/concurrency/stale/unrelated/smoke-failure/child cleanup |
| Actual rebuild | Clean application/root outputs and host objects/binaries; observe four links/three C++ producers, not cached staging-only artifacts |
| Runtime | Mac standalone offscreen Canvas/kRPC/Flow/Unicode/security seven; Windows/Linux GUI and arm64 execution gaps |
| Configuration cache | Strict store/reuse; ValueSource observes actual PID even on hits |
| Archive cache | Removed frontend.pack restored FROM-CACHE, identical hash |
| SDK/offline | Warm offline resolution; removed headers FROM-CACHE with same hash. Modified header conservatively disabled caching/reexecuted/repaired, not falsely called a hit |
| Daemon lifetime | Stopped only task-owned idle daemon, minimal explicit bootstrap new PID, subsequent pack/configuration reuse |
| Release scope | Mac-only checksum passes/full manifest removed, then full release restores all four valid targets/sidecars |
| Non-GUI policy | Linux without display consent refused immediately, no workstation Gradle/window |

- Recorded warm builds of 57s, cache-hit 756ms and post-daemon-change 1s executed different work: not speed comparisons.
- [Validation history](validation.md) records artifacts; actual Mac Canvas sampled 110 colors.
- Evidence: build-governance-{rebuild,final,reuse,policy,pack-restore,sdk-restore,sdk-repair,unit-graph,help,bootstrap-reuse,new-daemon-reuse,mac-only}.log and module XML.

## Verified upstream limits

- kRPC 0.10.4 calls deprecated Project.getProperties from RpcPluginConstKt.isInternalDevelopment, established by deprecation trace, not newly added project logic.
- Keep Gradle 9.6.1 and warnings visible; a Gradle 10 upgrade needs upstream compatibility rather than an unverified Kotlin/RPC upgrade.
- No cross-machine cold-cache reproducibility or comparative build-speed experiment.
