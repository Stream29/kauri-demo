# Historical record: minimal host policy

This analyzes the third-party wrapper before direct Kotlin/Native AppKit/WKWebView, Win32/WebView2 COM and GTK/WebKitGTK migration. It does not prove current runtime coverage.

## Profiles and offscreen testing

Date: 2026-10-21. Analysis used pinned webview 0.12.0, the then-current host and official documentation. No new build or host change ran in that analysis.

Subsequent implementation was authorized; this remains pre-migration context. See [historical implementation](minimal-static-host.md) and current checklist/architecture.md.

## Confirmed first-phase scope

- Compose Wasm owns page routing/back stack and in-content dialogs; no second native routing/dialog system.
- Native popup/multiwindow support is out of scope. Rejecting unexpected windows is a boundary rule, not multiwindow implementation.
- The WebView must still prevent documents leaving the app origin. This is not Compose routing and cannot be ensured merely by never explicitly navigating.

## Profiles are browser data, not application accounts

- They include cookies, cache, localStorage, IndexedDB and some permissions. Native persists business notes/settings; the two stores need not be merged.
- Single-file distribution does not mean zero runtime data directories.
- No multi-profile manager, account switching, data migration or uniform cross-platform cache layout was required.
- The historical recommendation was one ordinary application context and an independent writable location only when needed. It was a proposal then, not an implemented/user-confirmed default.
- Launch ports are random. localStorage is origin-scoped, so a fixed profile does not guarantee restoration across different ports. Business persistence remains Native over kRPC; do not fix the port for storage.
- Cookie isolation differs from localStorage; random ports do not isolate all browser state.

### Pinned defaults and configuration timing

| Platform | Pinned wrapper behavior | Minimum customization and limit |
| --- | --- | --- |
| macOS | New WKWebViewConfiguration without websiteDataStore; system persistence default | Set nonPersistent before creating WKWebView; an existing native handle cannot replace initialization configuration |
| Windows | Explicit `%APPDATA%/<executable name>`, not SDK's adjacent default | Identity/temp directory must be selected before environment creation. SDK environment overrides were documented, but equivalence with the pinned built-in loader was untested; avoid global environment dependence |
| Linux | webkit_web_view_new uses default context | Ephemeral context/view is creation-time, not switchable afterward; exact data-path customization also needs a creation-time hook |

- Public C API lacked these pre-creation settings. A few patched construction sites or a small configuration entry could be evaluated instead of rewriting the host; neither was already a verified patch-free solution.
- Earlier Mac smoke/static-library probes isolated business notes but did not explicitly select nonPersistent WebKit storage. Their passes were not proof of browser-data isolation.
- Tests should use disposable contexts. Windows disk directories must be cleaned after browser/file release, not merely window destruction.

## Offscreen was a test mode, not the product renderer

- It meant a real system WebView running Wasm/rendering for capture without showing/activating user windows, not a replacement renderer, video stream or display-free runtime.
- Ordinary product operation needed one normal window, not a generic offscreen API.
- Automation needs target dimensions, event loops, background scheduling where needed, completion/deadline and cleanup. Hidden windows do not guarantee continuous Canvas/animation frames.
- Existing Mac caller-owned offscreen-window evidence was reusable only for Mac, not other platforms.
- Linux typically still needs a graphical session. An owned virtual display belongs to the runner, not the product framework.
- Static-library Windows/Linux GUI coverage was missing; no generic offscreen abstraction was justified.

## Complexity assessment

- For one local trusted UI with Native-owned state, profiles/offscreen could be bounded configuration/test adaptations, not sufficient justification for a general custom desktop host.
- Creation timing, callback lifetime, background painting and shutdown still needed validation without building profile/native-route/multiwindow frameworks.
- GTK/WebKitGTK cross-link SDK needs are independent of these runtime features; dropping routes/dialogs does not remove them.

## Sources

- [Apple store defaults/creation settings](https://developer.apple.com/documentation/webkit/wkwebviewconfiguration/websitedatastore).
- [Microsoft UDF data, permissions, sharing and cleanup](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/user-data-folder).
- [Microsoft environment creation and overrides](https://learn.microsoft.com/en-us/microsoft-edge/webview2/reference/win32/webview2-idl?view=webview2-1.0.2151.40).
- [GTK default context and is-ephemeral](https://www.webkitgtk.org/reference/webkit2gtk/stable/class.WebView.html).
- [HTML origin-scoped localStorage](https://html.spec.whatwg.org/multipage/webstorage.html#the-localstorage-attribute); random-port implications are project-specific inference.
- Pinned upstream source was resources/webview/core/include/webview/webview.h, including GTK/Cocoa creation and Windows embed.
- [Static-library probe report](static-webview-spike.md) records actual coverage; this analysis added no platform execution evidence.
