# Task Tree

- `Trace direct-host abstractions and state ownership`()
- `Independently review removable complexity`()
- `Delete redundant state and collapse repeated plumbing`()
- `Run four-target remote builds and existing regression gates`()
- `Record actual reductions, review findings and remaining runtime gaps`()

# Details

- User identified unablated design/abstraction redundancy in the new direct Kotlin/Native host. Simplify existing behavior, not a new framework or architecture migration.
- Baseline is Session 1034's repair: ten source hashes and four binaries were verified in the parent; Mac ordinary offscreen startup/close, smoke and security pass. Windows/Linux GUI runtime gates remain open.
- Primary candidates: Windows write-only handler fields, tag-to-IID indirection and repeated COM allocation plumbing; unused constructor properties; one-call forwarding helpers; test-induced redundant state. Confirm use sites before removal.
- Keep SDK-typed COM vtables, ABI signature, refcounts/StableRef ownership, late callbacks, loop-unwind cleanup, path/popup guards, isolated tests, application title and ordinary-startup coverage.
- Keep app/framework boundaries, Compose Wasm, typed kRPC and four single-file targets. Do not expand into configuration objects, plugin systems or a general COM library to save a few repeated lines.
- Read project/skill guidance; IDEA tool discovery currently returns no IDE MCP tool, so do not force an import or control the user's IDE. No workstation application compilation.
- Compare Mac/local hashes before selective sync. Build via `scripts/gradle-mac.sh` on `stream@macbook`, explicitly reuse detected daemon JVM, two workers. Existing daemon 45062 is pre-existing and must not be stopped by this task.
- Baseline and review evidence go in ignored `build/review/host-ablation/`; durable deletion decisions and actual verification go in `resources/`. Preserve previous release receipts as history when replacing current binaries.
- No dependency upgrades, Git reset/stage/commit, active-desktop GUI or unrelated cleanup. Remove only this task's temporary resources; do not clear shared SDK or compiler caches.

## Checkpoint

- Windows/GTK/Mac/common host changes net -97 Kotlin lines; four unused GTK header lines removed. First and actual-diff agy reviews completed; rejected false dead-constant and invalid `CValue.ptr` suggestions.
- Four-target compilation/release, Mac ordinary/smoke/security and eight Native tests passed. Resource 4/4 and policy 11/11 passed; initial extra-parent-lock policy invocation failed as designed and standalone rerun passed, with both logs retained.
- Strict configuration cache reused; unchanged four links remain up-to-date. No build configuration or runtime architecture changed.
- Durable findings in `resources/native-host-ablation.md`; four artifacts, receipt, source hashes and fresh logs synced and checked. Final wording review completed; temporary source-transfer/extraction copies removed, evidence retained, pre-existing daemon untouched. Windows/Linux GUI runtime acceptance remains open in the separate migration task.
