# Streaming embedded resources

Historical release baseline: native offscreen behavior and browser persistence options were subsequently removed. Source remains the resource API, but the hashes/GUI results below belong to this earlier build; current delivery is in [fixed disposable context](ephemeral-browser-context.md).

Status: 2026-10-22, implemented and release-tested on Mac. Public API is `openEmbeddedResource(path): kotlinx.io.Source?`; the whole-resource ByteArray reader is removed.

## Contract

- Lookup validates the package metadata without reading all payload bytes. Each open creates an independent cursor over a read-only process-image slice.
- Native reads copy at most 8 KiB into a kotlinx-io buffer segment. No whole-payload ByteArray allocation or eager background producer; lookup still allocates small resource-name metadata.
- Caller owns Source and closes it with use; close is idempotent, reads after close fail, EOF and zero-byte requests follow RawSource semantics. Process-image storage is never freed by a reader.
- Source is a single-consumer standard reader, not a thread-safe cursor. Consumers can deliberately request read-all; the application HTTP path does not.
- This bounds payload buffering in this integration, not total RSS, OS file-cache residency or browser decoding/compilation memory. The executable still contains the complete resource pack; closing a reader does not unmap it.

## Ktor composition

```kotlin
val source = openEmbeddedResource(path)
if (source == null) call.respond(HttpStatusCode.NotFound)
else source.use { call.respondSource(it, contentType) }
```

- Framework exposes standard kotlinx-io 0.8.0 as an API dependency, matching the version already used by pinned Ktor; it does not depend on Ktor.
- Leave contentLength unset for the pinned version. HTTP/1.1 uses chunked transfer; this is valid for the embedded Wasm/JS/font assets. The caller owns closing; no custom adapter, producer coroutine or new resource DTO is needed.

## Pinned source evidence

Inspected cached official source artifacts on 2026-10-22: Ktor 3.3.0 and kotlinx-io 0.8.0, which is already Ktor's dependency. Public docs below may describe newer releases; implementation decisions use the pinned artifacts.

- [Source](https://kotlinlang.org/api/kotlinx-io/kotlinx-io-core/kotlinx.io/-source/) is the standard buffered reader; RawSource supplies bounded reads, buffered() gives the ergonomic Source API.
- [Ktor respondSource](https://api.ktor.io/3.3.x/ktor-server-core/io.ktor.server.response/respond-source.html) accepts RawSource directly. In 3.3.0 ApplicationResponseFunctions.kt the no-length path calls writeBuffer(source); the explicit contentLength path calls writeBuffer(source, length).
- Pinned ByteWriteChannelOperations.kt: no-length writePacket loops over buffered segments and flushIfNeeded, respecting downstream backpressure. Explicit-length writeBuffer first calls writeBuffer.write(source, length), then flushIfNeeded; this can accumulate the whole requested length. Leave contentLength unset rather than mistaking a Source parameter for proof of streaming.
- Pinned BaseApplicationResponse.kt awaits content.writeTo inside the response channel's scope; CIOApplicationResponse.kt also joins chunked encoding. source.use around call.respondSource therefore covers writes, early pipeline rejection and failure for this integration.
- Pinned UnsafeBufferOperations.writeToTail exposes only a bounded writable segment during its callback. Native memcpy writes there while the ByteArray is pinned; no native pointer escapes that callback.

## Verification

- All compilation ran on the remote Mac using Gradle daemon 6103, its detected GraalVM JDK 21.0.8 and two workers. No workstation Gradle/import or desktop control.
- Four release links actually executed: build 1m 37s, 91 tasks, 61 executed / 30 up-to-date. Same task group repeated in 676ms, 6 executed / 85 up-to-date, strict configuration cache reused and all four links up-to-date. These are build observations, not application performance comparisons.
- Forced Native rerun: 15 tests, no failures/skips. Three new native-source tests cover empty/lazy open, bounded reads, exact bytes/EOF, independent cursors, zero/negative requests and close. Two new Ktor-writer tests cover normal EOF and downstream stall/cancellation.
- The stalled ByteChannel consumed 1,048,576 bytes of an 8,388,608-byte generated fixture, then remained unchanged while the consumer did not read; cancellation closed Source. This tests the pinned no-length writer's backpressure, not total heap/RSS or a universal 1 MiB bound for all engines.
- Node resource-pack tests 4/4 and script-policy checks 11/11 passed.
- Forced IDEA model/binding preparation passed with prepared SDK inputs temporarily absent (36 tasks executed); the inputs were restored afterward. This was remote preparation, not a new workstation IDEA import.
- Mac copied-executable ordinary offscreen startup/close and real Compose Canvas (129 sampled colors), typed kRPC, Unicode note, Flow 1/2/3, native navigation/new-window guards, and cleanup passed.
- Seven HTTP security cases passed. New HTTP regression interrupted a 16 KiB/s client after 0.5s, then fetched the largest Wasm resource anew: 8,652,729 bytes, chunked, no Content-Length, byte-for-byte equal to its built asset. It demonstrates transfer/reopen integrity, not server memory accounting.
- Mac licenses output uses an explicitly closed text reader. Windows/Linux coverage remains compilation/link/format/dependency audit only; no target CLI/GUI execution is claimed.
- Evidence: `build/review/streaming-resources/` logs and test XML, `build/smoke/`, and current release checksums/receipt in `build/release/`.
- Release files and the complete source/build-script manifest were synchronized to the workstation and independently SHA-256 checked. Old release checksums/receipt were preserved under `build/review/atomic-rewrite/`.
- Prepared SDK inputs were restored and temporary locks released. Only this task's daemon 6103 was stopped after confirming ownership, JVM/version, IDLE status and last project; no stage/reset/commit. Completed task: `kanban/done/2026-10-22-streaming-resources.md`.

| Target | Release bytes | SHA-256 |
| --- | ---: | --- |
| macosArm64 | 17,043,248 | `549e963aab2653dbc90288dcb659831fd9fc6a5539b850710a7814954eb002b3` |
| mingwX64 | 19,704,832 | `9c6d9f38cd11c5aa196e737d4ebb2162445dd662e61714446451d2912f712f13` |
| linuxX64 | 18,804,376 | `3eeab09449aff4167c8d4e1a5337fee456183be9963e5167aa4cfb4ee3e88a6e` |
| linuxArm64 | 18,273,928 | `ec9841c3848cbdb30019aa85b3436ea0bac79a9ff4a49a65b67e2dff656863fb` |

- Dependency audit: Mac links system frameworks/libraries; PE imports system DLLs without WebView2Loader.dll. Both Linux executables require system WebKitGTK/GTK/GLib and libc-related libraries. These requirements are unchanged by streaming.

## Independent review disposition

- agy Gemini 3.8 Flash Low reviewed source/tests/contracts. Its five initial findings were withdrawn after checking pinned segment capacity guarantees, existing entry bounds, generated symbols and chunked response semantics; metadata indexing remains an unmeasured optimization, not a streaming prerequisite.
- No speculative abstraction or extra API was added. Source review and correction responses are in `agy-review.json` / `agy-review-corrections.json`; the later document review found no blockers. Advisory review does not replace platform execution.
