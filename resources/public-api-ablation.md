# Application integration API ablation

Historical record: the single-entry implementation below was subsequently removed and rewritten as user-approved atomic capabilities. See resources/atomic-rewrite.md and current validation; these signatures/receipts do not represent current delivery.

## Historical usage

```kotlin
fun main(args: Array<String>) = runDesktopApplication(
    args,
    name = "Kauri",
    service = DesktopService::class,
    createService = ::NativeService,
)
```

- Optional title defaulted to name. Factory type: (dataDirectory: String) -> Service, matching NativeService's constructor. One shared service was created per application run, not per WebSocket.
- service had to be the exact @Rpc interface. An actual negative compilation rejected NativeService::class.
- The frontend retained the shared contract and typed kRPC; framework contained no application business logic.

## Removed calling layers

- Before: application entry → DesktopApplication → createServer → LocalServer → Route/path callback → rpc/registerService.
- After this historical change: one non-inline launch function with an explicit contract and service factory, not an added facade over the old chain. DesktopApplication, external server factory and route callbacks were deleted.
- LocalServer remained internal and registered the protected RPC route. Assets, capability/path checks, notices and platform helpers became internal. This reduced public assembly depth, not underlying services or security.
- Application framework imports fell from three to one; Ktor server imports from one to zero. Handwritten public source symbols fell from eight to one entry, excluding generated SDK declarations.
- Ktor/kRPC server dependencies became implementation. No Builder, options object, compatibility overload or generic service registry was added; that version explicitly supported one contract. Application atomic-write temporary names and Windows directories stopped using public framework helpers.

## Actual verification and boundaries

- Four application targets compiled; four release links reran in 56s/91 tasks. Repeat: 608ms, all four links up-to-date, strict configuration-cache reuse. Not a performance-benefit measurement.
- Native 8, resources 4/4 and policy 11/11 passed. Mac standalone ordinary offscreen startup/close/finally, real Canvas/kRPC/Flow, input/security guards, seven HTTP checks and --licenses passed again.
- Ordinary name/title validation ran first. Framework tests covered pure validation; the real entry was exercised by the standalone binary rather than linking application/fake resources into tests.
- Initial compilation exposed a missing application Windows helper dependency; a private application directory implementation fixed it. A subsequent test link exposed the full launcher's resource-symbol dependency; the test boundary above fixed it. Failure and passing logs were retained.
- agy's initial design review incorrectly denied @Rpc on type parameters. Pinned 0.10.4 sources, four-target compilation and the negative test supported retaining that constraint. No speculative Closeable/multi-service expansion was added.
- Source manifest, negative fixture and review/build/runtime logs: ignored build/review/public-api. [Validation history](validation.md) records artifacts. Windows/Linux were compile/link only, not GUI runtime passes.
