# Task Tree

- `Inspect pinned kotlinx-io and Ktor source contracts`()
- `Replace whole-resource API with a lazy standard Source`()
- `Connect Ktor with backpressure and reliable source closing`()
- `Test bounded native reads and cancelled consumers`()
- `Build four targets and run Mac standalone regression`()
- `Record verified artifacts and release owned resources`()

# Details

- User rejects whole-file ByteArray resource reads and requires streaming into Ktor. Use one `openEmbeddedResource(path): kotlinx.io.Source?`, not a new resource DTO, background producer or Ktor dependency inside framework.
- Pinned Ktor 3.3.0 already uses kotlinx-io 0.8.0. Reuse that version as an explicit API dependency; inspect actual sources, not latest documentation alone.
- Native raw source copies only a bounded chunk from the process-image slice into an I/O segment. Opening creates a cursor, not a payload buffer; each open has an independent cursor. Caller closes with use; no freeing process-image memory.
- Pinned Ktor respondSource without contentLength uses writeBuffer/source buffering and flush backpressure. Its explicit-length path first writes that length into writeBuffer before flush; do not use that path for large assets. CIO awaits response writing before respond returns, permitting source.use around respondSource; errors/cancellation must also close.
- Verify native chunk/EOF/zero/closed/independent-reader behavior and Ktor stalled-consumer/backpressure/cancellation using bounded fixtures. No claim of socket zero-copy or measured total-process memory improvement.
- Build only on Mac using scripts/gradle-mac.sh; no workstation Gradle/import or GUI, no stage/reset/commit. Preserve unrelated staged/untracked work. Windows/Linux runtime gaps remain explicit.
- Checkpoint: four fresh release links, 15 forced Native tests, 4 resource and 11 policy checks passed. Mac standalone Canvas/kRPC/Flow/policy/security and interrupted/reopened chunked Wasm transfer passed; exact 8,652,729-byte comparison. Stalled writer consumed 1 MiB of an 8 MiB fixture and closed on cancellation; this is not RSS measurement.
- Strict configuration-cache reuse and no-prepared-SDK import preparation passed. SDK inputs restored; release/source manifests synchronized and SHA-256 checked locally. agy source findings withdrawn after pinned-source fact checks; document review has no blockers.
- Completed: only this task's daemon 6103 was stopped after verifying user/JVM/version, IDLE status and last project. No SDK temporary directory or lock remains. No commit or workstation Gradle/import; Windows/Linux runtime gates remain in the original migration task.
