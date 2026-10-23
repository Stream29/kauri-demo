# Direct Native host ablation

## Scope and actual deletions

- Baseline: Session 1034 repair delivery. Business behavior, targets, public configuration and build tasks were unchanged.
- Windows: removed five write-only handler-pointer fields. The handlers collection still records allocations; final COM Release still determines destruction. Reference counting was unchanged.
- Removed HandlerKind → IID mapping. Callbacks retain the SDK's static IID directly; GUID comparisons no longer allocate/copy fields.
- Consolidated five allocation-wiring sequences into one private newHandler. Each call still allocates its SDK vtable and typed Invoke; shared wiring fills only the SDK IUnknownVtbl prefix and StableRef ownership. This is not a generic COM factory or a handwritten four-slot table. See [COM layout](https://learn.microsoft.com/en-us/windows/win32/com/interface-pointers-and-interfaces).
- GTK window/close state now exists only in callback state. Removed duplicated constructor properties and three one-use function-pointer aliases. Mac inlined a one-use getter and made initialization-only offscreen state local. Removed four unused GTK typedef/blank lines and unused common cinterop opt-in/import.

| Kotlin host file | Baseline lines | After ablation |
| --- | ---: | ---: |
| mingwMain/DesktopHost.kt | 651 | 569 |
| linuxMain/DesktopHost.kt | 275 | 265 |
| macosArm64Main/DesktopHost.kt | 176 | 174 |
| nativeMain/DesktopHost.kt | 134 | 131 |
| Total | 1,236 | 1,139 |

- Net host reduction: **97 lines**. All framework Kotlin files fell from 1,645 to 1,548 lines. Line counts are not reliability or performance measurements.

## Retained behavior and review decisions

- Retained COM retain/release, StableRef, event-token removal, late-callback pumping, idempotent shutdown and safe DLL unloading at that historical stage. Single-window scope does not make asynchronous references disappear synchronously; [reference-count rules](https://learn.microsoft.com/en-us/windows/win32/com/rules-for-managing-reference-counts) still apply.
- Retained GTK policy/create guards, script completion and the then-existing smoke-only message bridge. Tests were not deleted or replaced with no-ops to reduce line count.
- Retained Mac ordinary startup/close regression and platform smoke isolation without new mode objects, generic options or plugins.
- Two agy Flash Medium read-only reviews proposed deletions and reviewed the actual diff. The second found no new blockers; this was not Windows/Linux runtime validation.
- The first report incorrectly classified the still-used RESPONSE constant as dead, supplied unusable CValue.ptr pseudocode and overstated some deletions as risk-free. Actual types, call sites and regression results took precedence.

## Verification and delivery

- Reused Mac daemon 45062/GraalVM JDK 21.0.8 with two workers. No workstation Gradle; the preexisting daemon remained running.
- Four framework targets/tests and four actual release links passed: 54s, 91 tasks (17 executed / 74 up-to-date). Repeating the same request took 575ms with four links up-to-date and strict configuration-cache reuse; not an ablation performance comparison.
- Eight Native tests reran, resource tests 4/4 and build policy 11/11 passed. An unnecessary extra parent lock initially caused the policy script's nested-lock rejection; the standard standalone entry passed without changing locking. Failure logs were retained.
- Mac single-file offscreen ordinary initialization/close/finally, Canvas with 110 sampled colors, typed kRPC/Flow, Unicode notes, validation, document/popup guards and seven HTTP security checks passed.
- Historical artifacts are recorded through [validation history](validation.md). build/review/host-ablation holds the preceding repair receipt, diff/line counts, review JSON and this iteration's build/runtime logs.
- Windows/Linux remained cross-compile/link coverage, not GUI acceptance. No generic abstraction, C/C++ host, wrapper or build task was added.
