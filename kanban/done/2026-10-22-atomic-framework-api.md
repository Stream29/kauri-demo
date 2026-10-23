# Task Tree

- `Record caller-owned composition root`()
- `Define and self-ablate minimal independent capability APIs`()
- `Review proposed contracts before runtime migration`()
- `Accept user confirmation and fresh-source rewrite`()
- `Rewrite resource reader and three platform windows from the specification`()
- `Rebuild demo composition and independent lifecycle tests`()
- `Validate independent use and existing release gates`()
- `Archive reviewed evidence and release owned resources`()

# Details

- User confirmed the opposite of framework-owned orchestration: only atomic capabilities, with the composition root owned by the caller.
- Target boundaries are in `checklist/architecture.md`; reference interpretation updated in `resources/compose-desktop-api-reference.md`. The previous application/rpc/window DSL is not adopted.
- User approved `resources/atomic-framework-api.md` and requested clearing the old implementation and rebuilding from the new specification. Resource reading and a scoped WebView window are the public capabilities; the app directly composes Ktor/kRPC.
- The app creates the business instance and chooses resource inputs, lifecycle sequencing and endpoint/window connection. A convenience root, if useful, belongs in the demo app rather than being the framework's mandatory public entry.
- Keep native capability-local security, thread/FFI ownership, callback release and reliable close. Under this proposal, existing server security integration moves intact to app-private code; removing a public wrapper does not remove loopback/capability/Host/Origin/path guards.
- Rewrite scope: remove the old framework runtime/host/configuration helpers and app startup root, not application contracts, Compose UI, native business storage or necessary SDK/build inputs. Prior source was already seen; this is fresh implementation from the specification, not a formal independent clean-room/IP claim.
- All application/native builds ran on Mac via the daemon-reusing helper. Four-target release, clean-output recovery, Mac regression, final notices and reviewed fixes are complete. Windows/Linux GUI coverage gaps remain explicit in the original direct-native runtime task; this completion does not close those gates.

## Rewrite checkpoint

- Removed every previous framework Kotlin runtime/host/helper and the app startup entry before implementing the new files; no compatibility launcher or renamed wrapper remains. Application contracts, storage and Compose content remain outside rewrite scope.
- Framework now has no Ktor/kRPC/coroutine/JSON runtime dependencies. App directly installs Ktor/kRPC and serves `readEmbeddedResource`; UI test completion uses a capability-header-protected app HTTP endpoint, not a native JS bridge.
- Resource cinterop now declares only start/end arrays; hand-written C inline functions were removed. Linux declaration header was trimmed and unused direct JSC/GDK/GIO/dl and Windows system link options removed.
- Mac release smoke passed real Canvas (128 colors), typed RPC/Flow, navigation/new-window rejection and seven HTTP security checks. Six framework, two app and two shared Native tests passed; four resource tests passed.
- Cocoa error cleanup is tested by injected SDK delegate failure while a real offscreen window runs, twice in one process. A non-listening local URL did not emit an error within the test deadline, so do not describe fault injection as real network-failure validation.
- Missing-prepared-input import preparation passed on Mac for framework and app (36 executed tasks); the SDK directory was restored by the owning shell. No local IDEA/Gradle import was triggered.
- Local workstation currently has user-owned IDE/compiler activity; do not start a Linux graphical runtime or VM session to fill coverage gaps. No user process/window has been controlled.
- Final binary/source manifests, screenshot and Native XML synced locally and verified. Release and warm-cache checks passed after final Mac teardown review; SDK-withheld import preparation reran successfully.
- agy source/document review and dispositions are recorded in `resources/atomic-rewrite.md`. Medium's unusable review was stopped; no background edits remain.
- Only task-created daemon 89815 was stopped after verifying user/JVM/last project/IDLE. SDK inputs restored, build lock released, shared toolchain caches retained; no commit or staging/reset.
