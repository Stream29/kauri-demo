# Atomic capability rewrite

Date: 2026-10-22. After approving two capabilities, the user requested removing the old implementation and starting again. Integrity is tied to the matching build/release/SHA256SUMS and VALIDATION.txt, not this narrative alone.

Subsequent changes replaced the whole-file ByteArray reader with streaming openEmbeddedResource. See resources/streaming-resources.md and current validation; names/hashes here belong to the rewrite baseline.

## Implementation boundary

- Removed all old framework Kotlin hosts, LocalServer, EmbeddedAssets, configuration/platform helpers and combined launcher, then wrote new files from the specification. No compatibility entry.
- Retained application DTOs/storage/Compose content, official SDK declarations, pack format and necessary toolchains; this was not an empty-window substitute.
- Earlier sources had been seen, so “rewrite” is not a formal IP clean-room/non-exposure claim.
- Public handwritten API then comprised readEmbeddedResource, WebViewWindow construction, run and requestClose. The window capability does not construct servers, invoke factories, own business directories or exit processes.

## Responsibilities

| File | Responsibility |
| --- | --- |
| framework/.../WebViewWindow.kt | URL/title validation, one run/active window, atomic close request |
| framework/.../EmbeddedResource.kt | Validate KAUR, then return an independent byte copy by name, without HTTP |
| framework/.../Window.macos.kt | AppKit/WKWebView, context, guards and message loop |
| framework/.../Window.windows.kt | Win32, SDK-typed COM callbacks, five-argument internal entry, bounded profile cleanup |
| framework/.../Window.linux.kt | GTK/WebKitGTK signals/sources/references and message loop |
| app/desktop/.../Main.kt | Caller composition: Ktor/kRPC, HTTP safety, resources, CLI/test lifetime |

- Native resources exist inside run(); requestClose only stores an atomic flag polled by platform UI timers. No cross-thread raw HWND/ObjC/GTK pointers escape.
- Natural/requested close ends the loop; application finally stops Ktor/coroutines. Cocoa/COM/GTK callback failures are retained and thrown after controlled cleanup.
- Initial URLs require explicit loopback ports. Navigation accepts normalized HTTP default-port omission; tests cover defaults, leading zeros and fake authority.
- No later navigation/evaluate/JS bridge, Ready/CloseReason streams, Profile manager or application DSL. Business/Flow remain typed kRPC; test HTTP completion is not a replacement business RPC.

## Removed layers and build inputs

- Framework no longer depended on Ktor/kRPC/serialization/coroutines; those belong to application composition.
- No server wrapper, public factory protocol, platform options, Smoke machine or second dispose/awaitClosed protocol.
- Resource header declares start/end arrays only. Kotlin computes the span/parser; generated cinterop bridges remain part of Native, with no handwritten C function.
- Linux removed unused script/evaluate declarations and explicit unused JSC/GDK/GIO/dl link statements. This did not remove browser/Native transitive system dependencies.
- Ordinary target-loop cinterop configuration declares resource/SDK headers as inputs. No build-logic/custom task/root aggregate added.
- Windows uses discovered trusted paths/restricted dependency search and caches modules for process lifetime. Removed unloading protocol avoids returning from final Release into unloaded DLL code; COM objects/events are still released.

## Verification

- Four actual release links; Mac copied one executable into an empty directory.
- Mac non-smoke startup/request-close, real Canvas (final 130 sampled colors), Native info, Unicode notes, oversize rejection, Flow 1/2/3, actual native document/popup guards and seven HTTP checks passed.
- Forced Native 10: framework 6, application storage/path 2, shared contract 2. Resources 4 and policy 11 passed; old XML was not substituted.
- Deleted four releases/pack/objects/frontend distribution and restored with build cache disabled: distribution/pack/four embeds/four real links ran, 57s. Later strict configuration reuse/four up-to-date links, final Gradle repeat 564ms.
- Temporarily absent build/native-deps: framework/app prepareKotlinIdeaImport forced 36 tasks successfully, then restored inputs. Remote preparation, not actual local IDEA sync.
- Windows/Linux were cross-compile/link only, not CLI/GUI. Existing workstation IDE/compiler activity was not disrupted to launch browsers/VMs.
- Source comparison found stale remote wrapper notices. Corrected/repacked all four, reran Mac and --licenses to verify embedded—not adjacent—notice content.

### Final historical artifacts

| Target | Bytes | MiB | Coverage |
| --- | ---: | ---: | --- |
| macosArm64 | 17,043,232 | 16.25 | Standalone offscreen UI/RPC/safety |
| mingwX64 | 19,693,568 | 18.78 | Compile/link/PE imports |
| linuxX64 | 18,793,032 | 17.92 | Compile/link/ELF dependencies |
| linuxArm64 | 18,262,352 | 17.42 | Compile/link/ELF dependencies |

- Paths: build/release/<target>/Kauri, Windows Kauri.exe. Independently copyable, still requiring system WebView/GUI runtimes.
- SHA-256: Mac `505a83ddda1f96c044352e393c8173ff817d5148b681dcab280f7747bd02f722`; Windows `c5a7c1025b9c2e06d0b7cb09e3572aa024089a1f35c0be1ae63712e70a5ff7ce`; Linux x64 `3f39dd506643aee75604ba89a6b3f74b3d3cafce8dbf917c4490c090b96b03d0`; Linux arm64 `6820a68277975590c9c114066e89dc38ba9adc4d991ae48d2bf99c3d1effc5c9`.
- Formats matched. Windows imported system DLLs only, without loader/JVM/libstdc++ companions. Linux directly required WebKitGTK/GTK/GObject/GLib/runtime libraries; removing explicit flags did not remove libdl/transitive needs.
- Binaries, source manifest, screenshot/XML/logs were synchronized and checked locally. Source receipt: build/review/atomic-rewrite/SOURCE-SHA256SUMS; binaries use their release manifest/receipt.

### Fault-injection limits

- A real running WKWebView received an injected initial-navigation failure through its public Cocoa delegate, checking retained error, loop exit, cleanup and a subsequent window. No product test API.
- An initial non-listening loopback socket still loaded after 20s; arbitrary elapsed time was not treated as a guaranteed failure callback. Controlled injection tested cleanup; an external deadline only prevented hangs.
- This proves cleanup after the delegate receives failure, not all slow/unresponsive network timeouts or initialization failures. No built-in navigation timeout was added; callers arrange failure monitoring/close.
- Initial build/test failures and final fixes remain under ignored build/review/atomic-rewrite, not replaced with older artifacts.

## Independent review disposition

- Two agy tool-capable implementation attempts reported zero edits; the main session wrote the final code. Flash Medium returned no text, not a completed review.
- Flash Low provided new Windows/core reviews in agy-windows-low.json/agy-core-low.json. Findings were checked rather than accepting “Proof” labels.
- Accepted/fixed default-port normalization and clearing HWND user data/checking destruction. Removed unnecessary temporary-file attribute resets to avoid shared hard-link attribute effects; this did not confirm the report's “deleting a link deletes its target” claim.
- Rejected added layers: callback acquisition instead of a direct window, concurrent map for main-thread serialized module access, Profile/event protocols.
- Rejected false alarms: Long-validated Int offsets cannot overflow the checked span; zero-ref COM objects cannot be reacquired after destruction; clearing GTK's owned pointer in destroy leaks its reference; FALSE policy permits default use; no ARM32 target exists.
- Separate document review clarified link statements, Windows cleanup and fault injection, merged repetitive disclaimers and found no blocking document issue.

## Official basis and limits

Retrieved 2026-10-22; API evidence, not platform runtime certification.

- [IUnknown order](https://learn.microsoft.com/en-us/windows/win32/api/unknwn/nn-unknwn-iunknown)/[zero-ref Release](https://learn.microsoft.com/en-us/windows/win32/api/unknwn/nf-unknwn-iunknown-release) support SDK-prefix wiring/ownership, not AddRef after destruction.
- [DestroyWindow](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-destroywindow) sends synchronous destruction messages and can fail; [thread rights](https://learn.microsoft.com/en-us/windows/win32/procthread/thread-security-and-access-rights) identify 0x0800 as QUERY_LIMITED_INFORMATION, not QUERY_INFORMATION.
- [Hard links](https://learn.microsoft.com/en-ca/windows/win32/fileio/hard-links-and-junctions)/[DeleteFileW](https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-deletefilew): deleting links differs from resetting shared attributes. Cleanup neither traverses reparse points nor resets attributes.
- [Internal environment entry](https://learn.microsoft.com/en-us/microsoft-edge/webview2/reference/win32/webview2-idl?preserve-view=true&view=webview2-1.0.2420.47) documents bool/int/UTF16/options/handler and possible change/removal; the user accepted that risk.
- [Kotlin 2.4 UUID random](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.uuid/-uuid/-companion/random.html) identifies Native CSPRNG. Two v4 UUIDs provide at most 244 random bits for a capability, not a cryptographic key/256-bit-entropy claim.
- [ref_sink](https://docs.gtk.org/gobject/method.Object.ref_sink.html), [GtkWindow initial reference](https://docs.gtk.org/gtk3/ctor.Window.new.html), [default WebKit policy](https://webkitgtk.org/reference/webkit2gtk/stable/signal.WebView.decide-policy.html) support explicit ownership/default FALSE handling, not discarding owned references.

## Missing coverage

- Windows internal loading, real COM/resize/late creation/UDF deletion require Windows 10/11 runner. Bounded retry/error/state retention does not guarantee deleting Edge-locked files or silently certify cleanup.
- Both Linux architectures need isolated GTK/WasmGC/render/close execution.
- Mac tests used non-persistent browser data, not the ordinary persistent profile; the historical Windows executable-identity location did not promise old-directory migration.
- No performance comparison, universal WebView compatibility, reproducible binaries or absence of system runtime requirements.
