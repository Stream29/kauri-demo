# Task Tree

- `Trace application-facing types and nested registration`()
- `Replace object chain with one typed launch function`()
- `Compile all targets and run existing Mac regression gates`()
- `Update usage, evidence and artifact receipts`()

# Details

- User clarified that ablation should reduce public abstraction depth, not merely internal line count.
- Current chain: `runDesktopApplication → DesktopApplication → createServer → LocalServer → Route/path → rpc/registerService`.
- Target: one non-inline `runDesktopApplication(args, name, service: KClass<Service>, title = name, createService)` function. Caller supplies an exact `@Rpc` interface and constructor; the framework owns one service instance, the private RPC route and server lifecycle.
- Delete `DesktopApplication`; make server/assets/notices/path/random helpers internal. The app's atomic-write temporary names use Kotlin UUID directly rather than importing a framework capability helper.
- No compatibility wrapper, builder, service registry, new public configuration object or generic launcher overload. Keep a single service contract for the current application; do not invent future multi-service configuration.
- Preserve secure name/title validation before side effects, app-owned data path, one shared service instance per launch, typed kRPC/Flow, title propagation, single-file targets and offscreen/ordinary regression paths.
- Read architecture and skills. IDEA MCP discovery is unavailable; do not trigger a local import or application build. Mac has no running Gradle daemon at initial inspection; helpers may bootstrap an explicit JDK only if that remains true.
- Build only on `stream@macbook` using `scripts/gradle-mac.sh`, detected/explicit JVM and two workers. Preserve unrelated work; compare hashes and selectively sync under the project lock.
- Evidence under ignored `build/review/public-api/`; maintain prior receipts as history, update current artifacts only after actual checks. Windows/Linux runtime gaps stay in the original migration task.

## Checkpoint

- Replaced the public object/server/route chain with one typed KClass entry; made helpers internal and server dependencies implementation. App Main now imports one framework function and no Ktor type; explicit implementation-class registration fails compilation.
- Four-target compilation and release, Mac ordinary/smoke/security/CLI, 8 Native tests, resource 4/4 and policy 11/11 passed. Strict cache reused and unchanged links up-to-date.
- Initial Windows helper-import and asset-linked unit-test failures were fixed without reopening helper visibility or adding fake embedded assets; logs retained. Pure validation tests and real standalone startup cover their respective boundaries.
- Artifact/source checksums downloaded and verified; actual-source and document reviews completed. Usage and context updated; negative fixture removed from the application source tree, transfer/extraction copies cleaned, and only the task-created idle daemon 84337 released after JVM/PID/last-project checks. Windows/Linux runtime gates remain in the migration task.
