# Task Tree

- `coroutineScope {`
  - `val independentReview = async {`
    - `Ask agy to audit the supplied baseline without edits or builds`()
  - `}`
  - `Audit Gradle logic, toolchain inputs, and release scripts`()
  - `Reconcile independent findings with observed evidence`(independentReview.await())
- `}`
- `Implement focused build-system improvements`()
- `try {`
  - `Compile and test build logic on remote Mac`()
  - `Verify configuration reuse, task invalidation, and local build cache`()
  - `Build four release targets and run offscreen smoke/security checks`()
- `} finally {`
  - `Clean owned test resources without stopping unrelated daemons`()
- `}`
- `Review build documentation and record remaining limitations`()

# Details

- User request: govern the build system toward best practices and obtain an independent agy review.
- Preserve the confirmed app/framework boundary, four Native targets, embedded single-file delivery, typed kRPC, and system WebViews. No runtime rewrite or dependency-version upgrade is implied.
- All Gradle/native compilation stays on `stream@macbook`; detect/reuse daemon JVM and cap workers at two.
- Baseline weaknesses to investigate: script-plugin/extra-property handshake, duplicated version pins, cross-project output paths, script-captured task execution state, hidden compiler/SDK inputs, and duplicate release orchestration.
- agy receives baseline files independently with explicit read-only/no-tools instructions. Its raw response and logs remain ignored under `build/review/`; only verified findings enter durable context.
- IDE MCP was unavailable during inspection. Do not take control of an IDE or desktop to work around it.
- Do not edit `kanban/Draft.md` or create a Git commit without a request.
- agy baseline review completed using Claude Sonnet 5.5 Medium, read-only and without tools; it returned 10 findings. Confirmed issues include hidden toolchain/SDK inputs, path/task-name coupling, daemon selection policy, duplicate release paths, and failure diagnostics. Several API/memory/strip concerns were explicitly speculative and are not treated as defects.
- Build logic is an included build with five typed plugins. Version catalog, target matrix, native compatibility descriptors, artifact variants, cacheable archive/header tasks, and local-only native/signing tasks replace script-plugin/extra-property coordination.
- SDK download/external caching now uses Gradle's restricted artifact-only NuGet/Ivy repository; extraction verifies the pinned SHA-256.
- Strict configuration-cache testing caught module-level Node download configuration missing from the initial root-only listener; convention plugins now configure those module instances.
- A task-graph inspection caught KGP's raw binary-file provider losing producer metadata at staging. Staging now explicitly depends on its typed link-task provider; native objects and outgoing artifacts also carry producer metadata. Earlier staging-only success is not accepted as a rebuild.
- Operational locks/logs/owned-PID tracking moved to `.gradle/kauri/` so `:clean` cannot erase a live invocation lock.
- The current validation clears root/application outputs before rebuilding; task arguments use qualified paths to avoid recursively selecting browser and cross-target check tasks.
- Completed both independent agy source reviews. Added exact manifest-directory validation and its regression test after the second review; closed routing/lock cleanup issues and documented unproven hypotheses rather than treating every model severity label as fact.
- Final checks: 14 build-logic unit tests, plugin validation, 6 Native tests, 11 script-policy checks, all four release links, Mac standalone offscreen Canvas/kRPC/Flow, and 7 security checks passed.
- Verified configuration-cache storage/reuse, matching archive SHA after FROM-CACHE restoration, offline SDK recovery, Native-only unit-test graphs, and lazy strip discovery on `:help`.
- Deleted extracted SDK headers and restored them FROM-CACHE; a separate external-modification probe caused safe task re-execution and repair, not a cache hit.
- Verified Mac-only release removes the full manifest; full assembly refreshes global and per-target manifests. Refused incomplete matrices in the new unit test.
- Stopped only the task-owned idle daemon, then verified minimal bootstrap to a new PID and subsequent cache reuse. At final handoff the replacement owned idle daemon was also stopped; unknown/shared compiler services were not touched.
- Latest artifacts, smoke capture, and test XML are synchronized locally. All artifact hashes and per-target manifests passed; Windows imports and both Linux dependency tables were re-audited. linuxX64 `--licenses` exited 0 without GUI.
- Remaining coverage gaps: Windows/Linux GUI and Linux arm64 execution. Upstream kRPC's Gradle-10 deprecation remains explicit; no application dependency upgrade or runtime rewrite was mixed into this work.
- Documentation passed source/intent, ablation, and whole-document checks. Exact results and audit dispositions are in `resources/build-review.md` and `resources/validation.md`; workflow is in `checklist/build.md`. No Git commit was requested or created.
