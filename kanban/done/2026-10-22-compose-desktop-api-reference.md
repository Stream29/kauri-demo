# Task Tree

- `Read official application and Window APIs`()
- `Compare lifecycle, window and service responsibilities`()
- `Review evidence and present the unimplemented proposal`()

# Details

- User asked to reference Compose Desktop API design after clarifying that fewer public abstractions, not just fewer lines, are the goal.
- Cache and proposal: `resources/compose-desktop-api-reference.md`. Sources are current official docs and `jb-main`, not a pinned 1.11.1 API-compatibility assertion.
- Current runtime/API remains unchanged. The application/rpc/window snippet is a proposal; do not treat it as an implemented API or confirmed architecture.
- Preserve Kotlin Native backend, Compose Wasm frontend, typed kRPC and single-window scope. No JVM Desktop host or cross-Wasm composable closure is introduced.
- No Gradle, GUI or device operations are needed for this reference analysis. No commit.
- Official-source comparison and bounded agy wording review completed. This task closes the reference analysis only; adopting or implementing the proposed API requires a subsequent decision.
