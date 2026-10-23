# Kauri

- Load skills associated with the work; keep planning, kanban, and user questions coordinated.
- Stay within the requested scope. Remove temporary files/worktrees and release owned resources when finished.
- Do not compete for shared compute or control the user's windows/devices. If the user takes over a resource, stop the affected operation.
- Prefer short unordered-list items. Reference files with URI links and relevant line numbers.
- Read `checklist/architecture.md` before changing runtime boundaries.
- Track work in `kanban/`; keep the active Task Tree resumable.
- Cache source/version findings in `resources/`; distinguish evidence from hypotheses.
- Build on `stream@macbook` in `~/ACodeSpace/demo/Kauri`, not on the user's Linux workstation.
- Before Gradle, detect the running daemon's JVM and explicitly reuse it. Never stop unrelated daemons.
- Use `scripts/gradle-mac.sh` on the Mac; it verifies daemon reuse and limits workers.
- Keep the backend Kotlin/Native, UI Compose Wasm, and application calls typed kRPC.
- Do not replace a failed integration with a JVM backend, browser-only UI, or handwritten RPC.
- Run GUI smoke tests only on an owned isolated graphical runner; never activate windows on the user's shared desktop. Without a runner, record GUI checks as not run.
- Record actual build, smoke, and size results; do not claim comparative performance without measurements.
- Never commit session capability URLs, local notes, or machine-specific credentials.
