# Pure Kotlin/Native host feasibility

Date: 2026-10-21. Based on Kotlin 2.4.0, WebView2 SDK 1.0.3650.58 and the pre-migration webview 0.12.0. ABI feasibility evidence, not a current wrapper dependency.

## Conclusion and scope

- **Key ABI probes support eliminating maintained handwritten C/C++ implementations.** Windows/Linux host migration and complete GUI validation were not completed in this exploration.
- Mac already used Kotlin/Native directly. Linux can call system dynamic libraries; Windows can consume SDK-generated C COM declarations.
- Windows loading is the central choice: porting existing logic to Kotlin retains an internal runtime entry, not an exclusively public Microsoft API path.
- Pure Kotlin/Native still permits SDK headers, generated cinterop bridges, the Native runtime and system libraries. It does not imply every executed machine instruction originates in Kotlin.
- No production source, Gradle configuration, release binary or architecture decision changed; no GUI window was activated.

## Actual probes

Compiled serially on Mac with existing Native CLI, explicit GraalVM 21.0.8, the project lock and 512 MiB–1 GiB JVM limits. No Gradle.

| Area | Actual result | Not established |
| --- | --- | --- |
| Linux x64 | Kotlin compilation; low-priority workstation execution passed GLib dynamic calls, Kotlin callbacks, StableRef state/destroy-notify release; resolved GTK/WebKitGTK construction functions | No GTK initialization/WebView creation, GUI/rendering/policy validation |
| Linux arm64 | Same sources cross-compiled to aarch64 ELF | No arm64 execution |
| Windows COM | Official WebView2.h C interfaces generated a klib; pure Kotlin PE x64 covered SDK vtables/callbacks, WNDPROC, COM methods and by-value RECT | No Windows execution, real environment/WebView creation or actual loader call |
| Resource symbols | Mac extern-array-only declaration accessed assembly-linked data; real execution checked four bytes and start/end difference | An interface replacement probe, not production repackaging |

- First Windows compile failed on GUID namespace; generated metadata showed platform.posix._GUID, and the next passed without handwritten GUID layout.
- EventToken.h remained a declaration-only compatibility header, not a C implementation; SDK/MinGW differences still need handling.
- Simplified QueryInterface/refcounts/mock calls were probe scaffolding, not shippable COM objects.
- No existing host.o/loader.o or precompiled WebView2 loader was used. PE imports lacked WebView2Loader DLL, which does not prove actual environment initialization.
- Evidence/source snapshots: ignored build/review/pure-native-*. Temporary klibs/programs/compiler directories were removed locally and on Mac.

## Windows loader: implementation language versus API boundary

Microsoft's documented deployment uses a WebView2Loader.dll or statically linked loader: [deployment](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/distribution), [single executable](https://learn.microsoft.com/en-us/microsoft-edge/webview2/how-to/static).

The [pinned upstream loader](https://github.com/webview/webview/blob/0.12.0/core/include/webview/webview.h#L3288-L3517) tries a loader DLL, then discovers installation via registry, loads EmbeddedBrowserWebView.dll and resolves CreateWebViewEnvironmentWithOptionsInternal.

| Route | No maintained C/C++ implementation? | Cost |
| --- | --- | --- |
| Kotlin calls official loader DLL | Yes | A companion DLL violates independently copied single-file behavior; extracting an embedded DLL was neither explored nor adopted |
| Kotlin links official static loader | No custom loader source required | Still a precompiled native dependency; earlier MinGW/MSVC link issues were not resolved here |
| Kotlin implements required built-in loading | Function-pointer/Win32 probes support it; loader itself was incomplete | Internal entry, registry/version/module compatibility and Windows execution become maintenance obligations |

- The third route could preserve single-file delivery without handwritten C/C++, but no evidence justified promising zero native loader dependency together with public-only initialization.
- Internal-entry risk already existed in the loader; porting changes its implementation/owner, not its origin.
- Do not guess COM layout/internal signatures. Generate public declarations from SDK and define trusted paths, version scope and failure diagnostics for internal loading.

## Potential removals after migration

- Candidates: kauri_linux.c, kauri_windows.c and kauri_loader.cc, with their manual compile tasks/object wiring.
- Remove the webview source dependency only after no code calls it. A port does not automatically remove attribution/license duties.
- Replace handwritten inline resource address functions with declared linker arrays; assembly embedding remains.
- Retain Compose Wasm compilation, pack/embedding, final Native links, system runtime requirements, SDK bindings and release signing. No C/C++ source does not mean no LLVM/cinterop.
- No post-migration time/size/maintenance measurements. Large SDK binding generation has cost; faster/smaller is not guaranteed.

## Required implementation checks

- Linux: verify a small exact set of exported function signatures. Macros cannot be dlsym targets; use functions such as g_signal_connect_data. Preserve guards/close order/async release and test both architectures.
- Windows: actual QueryInterface/refcount/lifetime for the five needed callback kinds, final instance/StableRef release, exceptions contained within C ABI, STA/UI pumping and non-reentrant close. See [threading model](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/threading-model).
- Loader: per-user/machine installations, missing/version-changing runtimes, absolute DLL paths and module retention during async work. Prove initialization before deleting the predecessor.
- End-to-end: redo standalone offscreen rendering, typed kRPC/Flow, safety and repeat startup/close. Compilation is not target execution.

## Interop basis and independent review

- Kotlin [C interop](https://kotlinlang.org/docs/native-c-interop.html) and [function pointers](https://kotlinlang.org/docs/mapping-function-pointers-from-c.html) cover noncapturing staticCFunction and StableRef user data.
- GLib [timeout_add_full](https://docs.gtk.org/glib/func.timeout_add_full.html) defines stop/destroy-notify; the running probe checked Kotlin release.
- GTK [evaluate_javascript](https://webkitgtk.org/reference/webkit2gtk/stable/method.WebView.evaluate_javascript.html) is asynchronous with finish ownership; a port must preserve lifetime.
- agy Gemini 3.8 Flash Medium read-only review confirmed evidence limits/internal-entry distinction; declaration/COM release/GObject macro clarifications were accepted.
- Windows arm64 was not added: requested Windows target is mingwX64. No generic framework for dozens of SDK events.
- Raw review: ignored build/review/agy-pure-native-feasibility.json, not target runtime proof.
