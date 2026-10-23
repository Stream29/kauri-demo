# Native offscreen API ablation

Historical delivery: browser persistence was subsequently removed in [fixed disposable context](ephemeral-browser-context.md). The hashes/receipt below belong to this earlier build.

Status: 2026-10-22, implemented and verified without GUI execution. Current binaries are listed below; native GUI acceptance remains pending.

## Confirmed boundary

- User approved deleting native offscreen behavior, not replacing it with an environment flag, internal setting or another public abstraction.
- `WebViewWindow(url, title, persistentBrowserData = true)` now has one normal display path per platform. Streaming Source and caller-owned service composition are unchanged.
- Removed Mac accessory activation/offscreen coordinates/orderBack, Windows no-activate style/offscreen coordinates/show mode, and Linux offscreen constructor/display-policy branch and declaration.
- Removed the frontend's smoke-only requestAnimationFrame timer override. Test and product pages now both use the browser's frame clock.
- Constructor ABI/source compatibility changes intentionally: the removed parameter has no compatibility alias. Repository call sites use the remaining options directly.

## Tests are not product features

- Compose scene/UI tests and browser-headless tests belong to the UI/browser layers; neither proves native-host creation, policy or destruction. This scoped deletion adds no new Compose/browser suite and makes no claim that such a suite has run.
- Mac/Linux native smoke scripts require `KAURI_ISOLATED_DISPLAY=1` on an owned graphical runner. This is an assertion of runner ownership, not a mechanism that creates isolation. Windows display behavior is also normal.
- Default release runs non-GUI Native/resource tests and Mac licenses/HTTP security/streaming checks. GUI is opt-in, and the release receipt explicitly records NOT RUN without a runner.
- Removed one Cocoa fault-injection unit test that depended on creating real offscreen windows. Ordinary unit tests still cover URL/policy predicates, construction, pre-run cancellation, repeat-run rejection and worker-thread rejection without creating a window. The former real delegate-failure cleanup case is now an explicit integration coverage gap, not a no-op or skipped test counted as passing.
- Native document/new-window callbacks, close requests and cleanup code remain. No owned graphical runner is available for this change, so current Canvas/kRPC/Flow, real native policy callbacks, ordinary GUI close and failure-callback cleanup require a new target run. Earlier [streaming evidence](streaming-resources.md) remains historical.

## Verification

- All four release links actually executed on stream@macbook: 1m 13s, 91 tasks (35 executed / 56 up-to-date). Gradle 9.6.1 used task-owned daemon 14515, its detected GraalVM JDK 21.0.8 and two workers. No workstation Gradle/import.
- Repeat of the same qualified tasks completed in 626ms with strict configuration-cache reuse, 6 executed / 85 up-to-date; all four executable links up-to-date. No comparative application performance claim.
- Forced Native rerun: 14 tests, no failures/skips (framework 8, desktop 4, shared 2). Earlier count 15 included the now-removed real-window Cocoa fault-injection test; the difference is explicit, not a fake pass.
- Resource-pack tests 4/4 and script-policy checks 12/12 passed. New policy case verifies Mac smoke rejects a missing runner before executing even the fake application. The existing fake-app failure-diagnostic case still passes without launching a GUI.
- Mac licenses CLI and seven HTTP security checks passed. A slow client was interrupted, then a new chunked response matched all 8,652,729 Wasm bytes. The Source stall/cancellation test still consumed only 1 MiB of its 8 MiB fixture before blocking.
- Release explicitly ran with `KAURI_ISOLATED_DISPLAY=0` and wrote GUI NOT RUN. No window, screenshot or native policy callback was exercised in this iteration; `build/smoke/` is earlier offscreen evidence only.
- Format/dependency audits confirm Mach-O arm64, PE x86-64 and both ELF architectures, with existing system framework/WebView requirements. Windows/Linux coverage is compile/link/audit, not CLI/GUI.
- Evidence: `build/review/remove-offscreen/` logs/test XML and source manifest; release receipt/checksums in `build/release/`. The previous release receipt/checksums are preserved under `build/review/streaming-resources/`.
- Current release files and complete source/build-script manifest were synchronized and independently SHA-256 checked locally. Only task-owned idle daemon 14515 was stopped after verifying user/JVM/version and last project; no temporary SDK/lock, workstation build or Git commit. Task: `kanban/done/2026-10-22-remove-offscreen.md`.

| Target | Release bytes | SHA-256 |
| --- | ---: | --- |
| macosArm64 | 17,043,248 | `dcea3c398e67010639514ce8a7c26a5352a3df40c3d293cc52612d48a9be8053` |
| mingwX64 | 19,704,832 | `94de7f2e1e9ea1e344bb837f4cc9177742638aedf2e35b6c7af4ba3010498864` |
| linuxX64 | 18,801,928 | `b9ee8943d74d501ba23f2919066af4d7a4f2d6d10ec0767a903757a3cb5bd906` |
| linuxArm64 | 18,270,840 | `83781afdaa9d5c2f935e7ca60d6298ce4e14f825bdd3eddb31ff4bb7c0013ce0` |

## Review disposition

- agy Gemini 3.8 Flash Low reviewed the submitted API, scripts, unit tests and README. One stale “Offscreen smoke exited” diagnostic was corrected before policy rerun.
- The report's objection to the conditional Linux smoke command is not a defect: its script already requires owned-display confirmation and a display, and README explicitly marks it as an unexecuted runner-only command. The report also mislabels the 14 total tests as belonging to AtomicApiTest; actual XML has 5 there and 14 across all suites.
- Runner ownership is an assertion, not an isolation implementation; documentation states this explicitly. No speculative replacement option or test abstraction was added.
- Final document review found no blockers. Clarified that the Linux command runs on a target runner, not the Mac builder, and distinguished the Windows cleanup contract from unverified runtime evidence. A reported broken code block was caused by the review's truncated excerpt; the complete specification's composition example is intact.

## Official reference scope

- [Compose 1.11.1 testing changes](https://kotlinlang.org/docs/multiplatform/whats-new-compose-111.html) describe v2 Compose UI test APIs. This matches the repository's Compose version.
- The [official Skiko test implementation](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui-test/src/skikoMain/kotlin/androidx/compose/ui/test/ComposeUiTest.skiko.kt) uses a raster Surface for scene rendering. The inspected jb-main source is not a pinned dependency or proof of native WebView integration.
- References were checked during the preceding design discussion on 2026-10-22; they motivate separating test layers, not changing backend/frontend technology.
