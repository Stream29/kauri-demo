# Historical record: unified static host

This records the pre-migration webview/webview static wrapper and decisions. Direct Kotlin/Native hosts replaced it on 2026-10-21. Current authority is [validation](validation.md), [architecture](../checklist/architecture.md) and the executable Task Tree. Archives/C API/resources/webview below are not current build dependencies.

## Historical implementation

Date: 2026-10-21. Architecture/Task Tree were updated before implementation, with no Kotlin/Compose/kRPC/Ktor/Gradle upgrade.

- All four targets used webview 0.12.0's static C API for windows, WebView lifetime and event loops. The former custom Win32/COM, GTK/dlopen hosts and loader adaptation were deleted at that stage.
- Kotlin/Native retained local Ktor, embedded assets and typed kRPC; Compose owned routing/in-content dialogs, not native routing/multiwindow.
- Platform code retained same-origin documents, popup rejection and data/test settings. Mac used Kotlin delegates; Windows/Linux policy shims compiled with the static wrapper.
- Ordinary operation used one context: default persistence on Mac/Linux and `%LOCALAPPDATA%/Kauri/WebView` on Windows, not an adjacent executable directory. Native notes did not depend on localStorage at random ports.
- Smoke selected non-persistent WebKit/ephemeral GTK and a Windows temp directory. Mac nonpersistent state was actually checked; Windows/Linux GUI/rendering/cleanup still required runners.
- The reference submodule was unmodified. Preparation verified its pinned header and patched three creation points in an ignored copy.
- Windows used SDK IIDs to avoid missing MinGW __mingw_uuidof specializations, not copied COM ABI or a new loader.

## Build simplification

- Removed build-logic convention/custom task classes. Modules directly declared KMP targets, binaries, cinterop, dependencies and linkerOpts.
- Root retained one apply false KMP classpath declaration; scripts selected module tasks, not root lifecycle wrappers. Deleting that declaration actually reproduced a SwiftPM build-service classloader conflict.
- KGP supplied Wasm distribution, Kotlin/cinterop compilation and final links.
- Extra wiring used one Exec asset packer and one Exec resource-object producer per target. Scripts atomically replaced outputs; asset/script/compiler/target inputs and link producers were explicit.
- Later IDEA repair separated bindings from linking: repository headers only for cinterop; historical .def declared -lwebview/system libraries. Prepared archives/SDKs became final binary inputs, not binding klib/IDE prerequisites.
- Experimental cinterop -linker-option -L arguments did not appear in klib manifests; final binaries used explicit standard linkerOpts rather than assumed propagation.
- Release scripts copied, stripped/ad-hoc signed Mac and created checksums/receipts. Signed temp files atomically replaced outputs with a stable identifier, not a running old inode. Ordinary links did not stage/sign.

## Explicit preparation

- prepare-native-mac.sh used KGP's distribution download when needed and konanc -Xcheck-dependencies, not a custom Native downloader.
- Downloads were SHA-256 verified; Linux used tested Debian bookworm locks. Prepared inputs lived under build/native-deps and downloads under user cache.
- Windows used the upstream MinGW-compatible built-in loader. Linux merged GNU libstdc++ object members into archives, while GTK/WebKitGTK remained dynamic system requirements.
- App Gradle builds did not call preparation implicitly; first use, native/SDK changes or removed outputs required an explicit rerun.
- The iteration used warm caches; it did not prove complete cold bootstrap on a fresh machine or cross-machine reproducibility.

## Verification and review

- Four releases linked. Mac standalone offscreen app, Native 6, pack tests 4 and backend security 7 passed: Canvas 110 sampled colors, notes/Flow/kRPC and document/popup rules.
- Repeat release/configuration reuse, policy 11 and standalone framework task graphs were checked. Historical sizes/hashes/coverage are in [validation history](validation.md).
- Two timed agy Flash Medium requests returned no text/zero tokens, not completed reviews. Flash Low supplied three build findings checked against actual graphs, strict cache, clean-output recovery and archive changes.
- The reviewer later withdrew those three findings. The then-current archive-in-klib approach was actually tested; subsequent IDE decoupling changed it to final-link inputs, so old probes do not validate new wiring.
- Later claims that FileTree was frozen at configuration contradicted Gradle live collections; SDK inputs were still shortened to inputs.dir. Two binary blocks respectively configured SDK search and resource object, not duplicate flags; four links did not reproduce the alleged object-path failure.
- Old convention caches/host outputs were cleaned and test XML archived. Input probes were restored/removed; only the task-created idle daemon was stopped. Reusable prepared/download inputs stayed; no commit in that iteration.
- Initial migration did not verify IDEA. Actual import and resource-width repair followed in [root-idea-sync](root-idea-sync.md), not inferred from CLI success.
- Windows/Linux GUI acceptance remained missing. Single-file delivery did not mean no system dependencies, better performance or greater reliability.

## Official references

- [KMP target/binary/cinterop DSL](https://kotlinlang.org/docs/multiplatform/multiplatform-dsl-reference.html).
- [Definition files/staticLibraries](https://kotlinlang.org/docs/native-definition-file.html).
- [Live Gradle file collections](https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_collections).
- [Pinned webview source](https://github.com/webview/webview/tree/0.12.0); [earlier isolated probe](static-webview-spike.md).
- [GNU libstdc++ licensing](https://gcc.gnu.org/onlinedocs/libstdc++/manual/license.html). This records components/sources, not a distribution-compliance certification.
