# Atomic APIs: design after ablation

Status: 2026-10-22. After user approval, the old framework was removed and rewritten, then resource access became standard Source and native offscreen/browser-persistence options were removed. See [disposable browser contexts](ephemeral-browser-context.md) for that iteration. Earlier rewrite/streaming/offscreen reports are historical baselines; this specification is not execution evidence.

## Criteria

- If Kotlin, Ktor or kRPC already provides the capability, do not add a same-shaped wrapper.
- A necessary capability does not imply its particular name/encoding is uniquely necessary. Necessity is evaluated against current single-window, single-file and non-interfering testing requirements.
- Fewer public symbols do not necessarily mean fewer layers. Application construction, arguments, startup order, failures and shutdown belong to the caller; platform ABI and native ownership stay inside the capability.

## Confirmed interface

```kotlin
fun openEmbeddedResource(path: String): kotlinx.io.Source?

class WebViewWindow(url: String, title: String) {
    fun run()
    fun requestClose()
}
```

- Two independent capabilities; no Application, Scope, Builder, Options, service factory or common resource base class.
- Window construction only validates/stores inputs. Native resources exist inside run(), which cleans them before returning or throwing. requestClose() is a request, not the completion boundary.
- There is no Kauri server API: callers use Ktor/kRPC directly.

## Candidate-by-candidate ablation

| Candidate | Replacement or loss if removed | Decision |
| --- | --- | --- |
| openEmbeddedResource | Application must interpret linker symbols/pack format or ship adjacent files | Keep standard streaming Source, not raw pointers/index/resource objects |
| WebViewWindow | Application must implement three native window/WebView APIs, COM/callback/thread lifetime | Keep one directly usable capability, not public Host/Controller/Engine layers |
| Initial url | Window cannot load content; implicit server discovery recreates a hidden composition root | Required; derive allowed origin from it instead of another origin parameter |
| title | Otherwise the demo name is hardcoded | Keep a string, not AppIdentity/WindowState |
| run() | Caller must drive platform loops, or construction implicitly blocks | Explicit synchronous lifetime of this window, not the whole application |
| requestClose() | Backend failure/test completion cannot end a blocking window without user interaction/killing | Keep thread-safe nonblocking request, not native destroy |
| Window close/dispose/awaitClosed/AutoCloseable | Resources already live inside run() and its finally | No second window destruction protocol; Source follows standard close |
| offscreen | Not needed by the product; Compose handles UI tests, owned runners handle native integration | Delete option and platform branches |
| persistentBrowserData | Native owns business state; no current browser-state restoration need | Delete option; fixed disposable contexts with internal Windows directory cleanup |
| LocalServer/RpcServer/registerService wrappers | Ktor owns engines and kRPC owns typed registration/routes | Exclude; application retains existing security composition privately |
| AssetStore/Resource/exists/list/MIME registration | Source, nullable lookup and Ktor responses suffice | No DTO or HTTP layer for resource access |
| navigate/evaluate/JS bridge | Constructor loads once; typed kRPC handles business; Compose handles routes/dialogs | Not public; test injection does not justify a product API |
| application DSL/launch function/CLI/directory and service factories | Ordinary Kotlin and application-owned argument/directory code compose these | Delete framework entry; demo helpers may be private |
| Close callbacks/state flows/native handles/multiwindow/tray/geometry | Natural close returns run(); programmatic close uses requestClose() | No speculative generic desktop framework |

## Behavioral contracts

### Embedded resources

- path is an exact packaged relative name. Found resources return independent Source cursors; missing ones return null. Opening does not materialize the payload; reads copy at most 8 KiB at a time.
- Caller closes with use. Readers are not required to be thread-safe and carry no MIME/HTTP metadata.
- No whole-file ByteArray reader or whole-file Buffer disguised as streaming. Consumers can explicitly read all; Ktor uses the no-length respondSource path and closes the source. See [streaming details](streaming-resources.md).
- Corruption fails explicitly rather than looking like a missing file. Pointers/index remain internal. An executable consuming this capability must link its asset object.
- HTTP decoding, normalization and rejection remain server responsibilities; exact lookup is not a path-security policy.

### Window lifetime

- Constructor validates url: current scope permits explicit-port http://127.0.0.1:<port>/... endpoints, rejects credentials/invalid ports, and never logs fragment capabilities in errors. Construction performs no network/native allocation.
- Top-level documents must remain same-origin; new windows are rejected. This is not Compose routing or server authorization; there is no public allowNavigation escape hatch.
- run() requires the platform UI main thread. Wrong thread, repeat run or another active window fails before native operations. An instance runs once and the process supports one active window. It creates/loads/drives the window until natural or requested close. Windows asynchronous environment/controller creation is driven within this loop, not assumed synchronous.
- requestClose() is thread-safe, idempotent and nonblocking. Before run(), it cancels display and the first run returns without a window; during run(), it requests shutdown; afterward, it is a no-op. It must not destroy native objects from a caller thread or callback stack.
- Initialization, loop exit and exception paths clean native resources. Exceptions cannot cross C ABI boundaries. Windows modules/static SDK vtables remain process-lived rather than being unloaded. Late creation callbacks have bounded cleanup; if unfinished, keep necessary state and report failure rather than claim profile deletion.
- Normal close returns Unit. Initialization, real initial-document failure or unexpected Web content process termination records a failure, ends the loop, cleans up, then throws. Closing/policy cancellation is not an engine failure. No Ready/CloseReason/event layer: page/business readiness is confirmed by the application protocol, not native document load alone.
- finally is guaranteed only for controlled return/throw, not a process crash or forced termination. Thread-safe requestClose does not imply POSIX signal-handler safety; applications must transfer signals to normal execution context.
- The window must not start/stop servers, invoke business factories, delete business directories or exit the process. Callers own other resources in an outer try/finally.
- No new Dispatcher, ApplicationScope or release token is required. A DSL cannot erase native thread/event-loop constraints.

### Fixed browser context

- Every run uses disposable browser state: Mac non-persistent WKWebsiteDataStore, GTK ephemeral context, Windows independent temporary UDF. No persistence/profileDirectory option.
- Business state and permissions belong to Native application code, independently of whether it executes on the main thread. Disposable browser state does not remove Origin/capability/navigation checks.
- Windows can write disk. Existing callback-lifetime and bounded-retry cleanup applies only to the directory this run created. Failures are explicit; unfinished callbacks retain the directory safely. No zero-write/forensic-erasure guarantee. Target runtime isolation/cleanup still needs a runner.
- Old persistent profiles are not read, migrated or deleted; Native business data is untouched. Fewer options/branches are not measured startup/RSS/I/O improvements.

## Caller composition

createDemoServer below is an application-private helper, not another framework entry; it uses Ktor/kRPC directly and preserves security checks:

```kotlin
val backend = createDemoServer(
    service = NativeService(dataDirectory),
    openResource = ::openEmbeddedResource,
)
try {
    backend.start()
    val window = WebViewWindow(url = backend.url, title = "Kauri")
    // The caller can give window::requestClose to its failure/test-completion code.
    window.run()
} finally {
    backend.stop()
}
```

- Construct the service first, then register directly with rpc { registerService<DesktopService> { instance } }; no extra Kauri generic service protocol.
- Callers can run only the server, read assets alone, or pass another compatible locally managed endpoint to a window.
- Application-owned logic includes loopback/ephemeral ports, launch capability, Host/Origin/RPC paths and static response/path checks. Reducing public indirection neither deletes these responsibilities nor permits removing existing safety gates.
- Private ABI/ownership helpers are allowed. Atomic APIs do not expose COM reference counting to applications.

## Acceptance

- Verify resource access without a window/Ktor, window use without a kRPC service, and caller-owned Ktor lifetime. Framework must not import application contracts.
- Test close requests before/during/after running, repeat/wrong-thread rejection, partial initialization failures and natural-close cleanup; not just a special smoke path.
- Real Canvas/kRPC/Flow, natural close and actual native document/popup callbacks require owned graphical runners. Without one, record gaps rather than substitute old offscreen binaries, HTTP 403 or Compose scene tests.
- Completion can use app-owned loopback test endpoints. Necessary internal policy observation/injection should not become a public API, nor should minimizing API size erase coverage.
- Rebuild four targets and verify incremental/configuration cache, embedding and header-only IDEA preparation boundaries. Separate runtime from compilation coverage.

## Sources and evidence limits

Design retrieval: 2026-10-22. These summarize official documents, not source quotations. The former LocalServer.kt/DesktopHost.kt/EmbeddedAssets.kt are removed; current execution limits are in [disposable contexts](ephemeral-browser-context.md).

- [Ktor code configuration](https://ktor.io/docs/server-configuration-code.html) supports loopback, port=0 and resolvedConnectors; [EmbeddedServer](https://api.ktor.io/ktor-server-core/io.ktor.server.engine/-embedded-server/index.html) already provides lifecycle. Current web docs may differ from pinned sources.
- [kRPC Ktor integration](https://kotlin.github.io/kotlinx-rpc/krpc-ktor.html) and [RPC servers](https://kotlin.github.io/kotlinx-rpc/rpc-servers.html) register contracts, not implementations. Documentation showed 0.10.3/0.10.4; the repository's 0.10.4 was compiled directly.
- [WKWebsiteDataStore](https://developer.apple.com/documentation/webkit/wkwebsitedatastore) and [GTK ephemeral context](https://webkitgtk.org/reference/webkit2gtk/unstable/ctor.WebContext.new_ephemeral.html) describe creation-time stores. GTK link evidence referenced 4.1/2.41.92 documents, not universal runtime compatibility.
- Native failure events exist: [WebView2 processes](https://github.com/MicrosoftDocs/edge-developer/blob/main/microsoft-edge/webview2/concepts/process-related-events.md), [WKNavigationDelegate failures](https://developer.apple.com/documentation/webkit/wknavigationdelegate/webview%28_%3Adidfailprovisionalnavigation%3Awitherror%3A%29?changes=l_5&language=objc), [GTK process termination](https://webkitgtk.org/reference/webkit2gtk/stable/signal.WebView.web-process-terminated.html). Their existence alone does not prove every host path is connected/tested.

## Specification-review history

- Self-ablation preceded an agy Gemini 3.8 Flash Medium reverse review (historical session e465e064-6f37-4291-adbb-012f9487bfda), not platform/code acceptance.
- Accepted explicit asynchronous initialization, thread rejection, failure versus normal close, and crash/signal cleanup boundaries; all use existing run() errors rather than another event layer.
- Rejected replacing a directly held window with a top-level callback that supplies its close function: that adds acquisition indirection without eliminating lifetime state.
- Rejected hiding resource access inside Ktor, requiring native Ready, or replacing offscreen with hidden display. These undermine independence, confuse business readiness or do not ensure real rendering.
- The user later removed offscreen and persistence options. Windows temporary disk data remains allowed; graphical isolation belongs to runners, not a window/profile framework.
