# Task Tree

- `Inventory first-party language and release inputs`()
- `Translate first-party source and documentation into English`()
- `Rebuild and verify four release executables on Mac`()
- **`Review translation and commit the English snapshot`()**
- `Create public Stream29 kauri-demo and push source/tag`()
- `Publish 0.1.0 with target archives and checksums`()
- `Verify remote commit and downloaded assets`()

# Details

- User first requested gh repository creation, push and a 0.1.0 release with build artifacts, then paused publication until all Chinese in the repository is translated into English and committed. Repository visibility was explicitly confirmed public.
- gh is authenticated as Stream29. No remote, release tag or GitHub repository has been created by this task yet. Existing local HEAD is the initial 11597e5 snapshot; do not rewrite it or publish before translation/review.
- Translate maintained README/resources and the Chinese Unicode test fixture; set the English UI document's language metadata to en. Preserve non-ASCII/Unicode test coverage and historical validation distinctions, hashes, dates, SDK sources and accepted internal-entry risk.
- Workflow skills are separately pinned upstream submodules. Do not modify their upstream content or rewrite their histories. Use an explicit Unicode-range inventory rather than Git's byte-mode Han regex, which falsely matched middle dots.
- Build only on stream@macbook using scripts/gradle-mac.sh, detected/reused daemon JVM and two workers. No owned GUI runner: GUI NOT RUN, no shared desktop activation. Existing artifacts are the previous disposable-context baseline until rebuilt after the HTML change.
- Publish four uniquely named target archives, archive and executable checksums, validation receipt and build/source provenance. Keep Windows/Linux execution gaps explicit; no performance claims, notarization or Authenticode claims.
- Never publish capability URLs, actual notes, credentials, raw local execution logs or temporary SDKs. Preserve existing application data and unrelated resources; remove only task-owned temporary files/locks/idle daemon. No push until the new English commit is ready.
- Checkpoint: 14 Markdown translations retain all historical 40–64 digit hashes and HTTP source links. Explicit Han-range scan passes for maintained files and workflow submodules. HTML en and an English emoji/accent fixture are the only source changes; actual embedded HTML was served and byte-compared with source without GUI.
- Mac four real links, forced Native 14, archive 4/4, policy 12/12, CLI/HTTP/streaming and strict configuration-cache reuse passed. No GUI. Actual macOS 26.5.1/Xcode 26.5 were checked. Task-owned daemon is 63182/JDK 21.0.8.
- Source-manifest comparison caught obsolete webview metadata only in the Mac mirror's .gitmodules; synchronized the correct four-module file and reran release consistency checks. Runtime code and target hashes are unchanged by that metadata correction.
- agy Flash Low reviewed the complete English documentation set and found no blockers. Clarified window versus Windows wording and the internal entry's compatibility description. Raw logs/review remain ignored.
- Remaining: verify synchronized source/artifact manifests, create the English commit, create public Stream29/kauri-demo, push main plus annotated 0.1.0, publish experimental preview with four archives/checksums/provenance, then download and verify every attachment.
