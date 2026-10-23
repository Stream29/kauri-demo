# Validation record

## 2026-10-23 current candidate: English-only 0.1.0

- First-party documentation is English. HTML language is en; the contract fixture uses English with emoji/accented Unicode rather than removing Unicode coverage.
- Four release executables were actually linked again on Mac. Forced Native 14, pack 4/4, policy 12/12, Mac licenses CLI, seven HTTP security checks and interrupted/reopened exact-byte Wasm streaming passed.
- Strict configuration-cache/incremental reuse passed. Build: 1m 3s, 91 tasks (22 executed / 69 up-to-date); repeat: 939ms, 6 executed / 85 up-to-date. These are warm build observations, not comparative application performance.
- **GUI NOT RUN**: no owned graphical runner. Browser-context isolation, cleanup and real native policy/Canvas/kRPC/Flow require fresh target runs. Windows/Linux coverage is compilation/link/format/dependency audit, not CLI/GUI.
- Bytes: Mac 17,043,248; Windows 19,701,760; Linux x64 18,801,520; Linux arm64 18,270,232.
- Current candidate hashes/limits are in [0.1.0 notes](release-0.1.0.md), ignored build/review/english-release and the matching build/release/VALIDATION.txt. GitHub publication is tracked separately in the release Task Tree.

## 2026-10-22 historical: fixed disposable browser contexts

- Removed persistentBrowserData; window construction takes url/title. Mac/GTK are non-persistent, Windows uses a unique temporary UDF. Removed identity/persistent-directory logic, retaining partial-init/callback/bounded cleanup.
- Native state/authorization/Source remained unchanged; old profiles were not migrated/deleted. No measured startup/RSS/I/O benefit; Windows can still write disk.
- Four actual links, forced non-GUI Native 14, resources 4/4, policy 12/12 and Mac CLI/HTTP/streaming passed; strict cache/incremental reuse.
- GUI not run; context isolation/cleanup still needed owned target runners. Windows/Linux compile/link/dependency audit only.
- Historical bytes: Mac 17,043,248; Windows 19,701,760; Linux x64 18,801,552; arm64 18,270,232. [Context report](ephemeral-browser-context.md) has hashes/review. Current receipt has been replaced.

## 2026-10-22 historical: native offscreen removal

- Removed public/platform offscreen branches and test frame-clock override without a hidden replacement. Source/composition remained unchanged.
- Four links, forced non-GUI Native 14, resources 4/4, policy 12/12, Mac licenses/seven HTTP/streaming and strict cache reuse passed.
- GUI NOT RUN. Removed one Cocoa fault-injection test that created a window; native-lifetime GUI coverage remained open. Old screenshots/Canvas/Flow were not current acceptance.
- Bytes: Mac 17,043,248; Windows 19,704,832; Linux x64 18,801,928; arm64 18,270,840. Windows/Linux compile/link/audit only.
- [Offscreen report](offscreen-ablation.md), ignored build/review/remove-offscreen and preserved historical receipt record that delivery.

## 2026-10-22 historical: streaming resources

- Replaced whole-file ByteArray with openEmbeddedResource: Source?. Native fills at most 8 KiB/read. Application uses Source.use/respondSource without pinned Ktor's eager explicit-length branch.
- Four actual links, forced Native 15, resources 4/4, policy 11/11, Mac standalone offscreen Compose/kRPC/Flow/native guards and seven HTTP checks passed.
- Backpressure/cancel close plus interrupted/reopened chunked exact-byte 8,652,729-byte Wasm passed; no total RSS/zero-copy claim.
- Bytes: Mac 17,043,248; Windows 19,704,832; Linux x64 18,804,376; arm64 18,273,928. Cache/incremental passed; Windows/Linux compile/link/audit only.
- [Streaming report](streaming-resources.md)/build/review/streaming-resources hold pinned-source evidence/hashes/review; its receipt is historical.

## 2026-10-22 historical: atomic rewrite

- Framework exposed resource/window/run/requestClose only. Application directly composed Ktor/kRPC; old launcher/server wrappers/hosts were deleted.
- Four actual links, Mac standalone offscreen Canvas/kRPC/Flow/native guards/seven HTTP checks, Native 10, resources 4 and policy 11 passed.
- No-build-cache clean-output recovery, configuration/incremental reuse and missing-prepared-input import preparation passed. No workstation Gradle or active IDEA import.
- Windows/Linux compile/link/audit only. Cocoa cleanup used injected delegate failure, not a real network-timeout proof.
- Bytes: Mac 17,043,232; Windows 19,693,568; Linux x64 18,793,032; arm64 18,262,352. Downloaded binary/source manifests verified.
- [Rewrite report](atomic-rewrite.md)/build/review/atomic-rewrite hold historical hashes/limits/review, not the current receipt.

## 2026-10-21 repair baseline: historical, not current acceptance

All application/host builds ran on stream@macbook:~/ACodeSpace/demo/Kauri, no workstation Gradle. Earlier static-webview probes did not certify the migration.

- Session 1034 repaired Session 1029's delegated implementation in the same architecture. The latter's separate failed agy quota request did not mean delegation never occurred.
- Ordinary startup created Smoke only for smoke and used application titles. Windows fixed the verified five-argument ABI, COM/token/resize/async-init cleanup and then-safe unloading. Linux fixed policy 0/1/2, real JS evaluation and smoke-only message bridge. Mac window delegate stopped the loop without terminate bypassing finally.
- [Detailed review/primary references](direct-native-review.md) was updated; no Windows/Linux isolated GUI runner. Original migration Task Tree remained executable.

### Repair build/regression

- Helper explicitly reused daemon 45062/GraalVM 21.0.8/two workers. No workstation import/build, user IDE shutdown or unrelated-daemon stop; compared files before selective sync, no blanket deletion.
- First four links: 55s, 91 tasks (17 executed / 74 up-to-date). Further Windows/Linux changes relinked; final delivery: 31s, 91 tasks (15 executed / 76 up-to-date), strict cache reuse.
- Repeat same seven qualified tasks: 641ms, 91 tasks (6 executed / 85 up-to-date), four links up-to-date; incremental-delivery.log.
- GTK header changes initially exposed stale cinterop up-to-date state. Added repository header inputs to existing two cinterop tasks; both Linux compiles/links then passed. IDE bindings stayed free of prepared archives/SDKs; no new build-logic/custom task/root aggregate.
- Moved only owned Mac frontend.o with fallback: link automatically scheduled embedMacosArm64Frontend, restored identical bytes, links up-to-date/cache reused, 581ms. Removed backup without clearing SDK/history.
- Native 8: framework 5 (including C five-argument forwarding/GTK policy 0/1/2), shared 2, storage 1. Also directly ran current Mac test binaries, not first-pass receipts.
- Node 4/4, policy 11/11 and Mac --licenses passed; no other target CLI execution.
- Mac function-pointer checks plus Apple clang 21 static assertions/IR for x86_64-w64-windows-gnu established bool8/enum32/wchar16/HRESULT32/pointer64 and argument shape. Declaration/compilation evidence only; no handwritten C/C++ host or real Windows internal-entry call.

### Subsequent historical ablations

- [Host ablation](native-host-ablation.md): four host files 1,236→1,139 lines, net -97. Removed duplicate state/tag/allocation wiring while retaining lifetime/security.
- Four links/Mac ordinary offscreen close/finally/Canvas/kRPC/Flow/policy/HTTP seven, Native 8/resources 4/policy 11 passed. Build 54s, 91 tasks (17/74), repeat 575ms/up-to-date/cache. Not performance measurement.
- Evidence: build/review/host-ablation. Daemon 45062/JDK 21.0.8/two workers reused; no local Gradle/import. Prior repair receipts retained separately; Windows/Linux gaps unchanged.
- [Public API ablation](public-api-ablation.md): removed DesktopApplication/public server factory/Route callbacks for one non-inline typed entry, one service construction per run; native lifetime/security unchanged.
- Four links: 56s/91 tasks (13/78); repeat 608ms/91 (6/85), cache/four up-to-date links. Native 8/resources 4/policy 11 and Mac ordinary offscreen/Canvas/kRPC/Flow/HTTP seven/--licenses passed.
- Application compiled with framework/shared only and no Ktor server imports; negative compile rejected implementation registration; framework tests needed no assets.
- With no daemon, explicitly bootstrapped task-owned 84337/JDK 21.0.8 then reused it. Evidence: build/review/public-api; no local Gradle/import. Stopped only that verified-idle matching daemon and removed own probes/transfers/locks.

### Historical public-API-ablation files

Mac/local receipts matched; all four downloaded SHA checks passed. Paths below are relative to build/release; this table is not current delivery.

| Target/path | Bytes | SHA-256 | Actual coverage |
| --- | ---: | --- | --- |
| macosArm64/Kauri | 17,010,288 | `29e35cdd854e929417e9af39f364727f02548aa944b3a827f138bc1f328ea663` | Native link/CLI/ordinary offscreen/real GUI safety |
| mingwX64/Kauri.exe | 19,620,352 | `66cbe61268a2abf91aa47a02b221f1f7f929c5edd88a5759f6f2574598aa8340` | Cross-link/PE, no CLI/GUI |
| linuxX64/Kauri | 18,774,120 | `e3d9c335e47f15673a6bde6e8d06be8f07cd01ef81fe812930333918a1f6dbdd` | Cross-link/ELF, no CLI/GUI |
| linuxArm64/Kauri | 18,247,016 | `2e372845c89abd7fa542eda76871ff957bbc3d2ce07da82ea5dd61d1159b6ae9` | Cross-link/ELF, no CLI/GUI |

### Repaired Mac runtime evidence and remaining gaps

- Ran one copied executable from an owned empty temporary directory, no resources/activation/shared-desktop takeover.
- Ordinary --startup-check omitted --smoke/--result-dir, kept ordinary URL/default WebKit profile/no Smoke state, isolated business data, used offscreen placement and performClose after actual load. KAURI_STARTUP PASS/exit 0, not manual desktop interaction.
- Smoke: Canvas 110 colors, Native systemInfo, Unicode round-trip/oversize rejection, Flow 1/2/3, RPC 403, actual foreign navigation/popup rejection, policyVerified=true.
- Ordinary/smoke exited naturally. KAURI_CLEANUP PASS was outside common finally after host/server/temp cleanup; failed Mac directory deletion throws rather than printing success. No terminate-as-cleanup substitute.
- HTTP seven: health 200, foreign Origin/Host/missing WebSocket Origin/invalid capability 403, raw/encoded traversal 404.
- build/review/direct-native-repair contains release-delivery.log, smoke/{startup.log,process.log,result.txt,window.png}, tests-*.log, policy.log, incremental-delivery.log, producer-recovery.log and ABI IR. No capability URLs/credentials.
- Windows still needed installed Runtime/internal entry, COM/token/late callbacks/resize/ordinary close/Compose safety execution. Historical 5s retention/safe module policy was not a no-leak guarantee across Runtime versions.
- Linux x64/arm64 needed owned displays for response loading, then-existing evaluate/message bridge, WasmGC/Canvas/kRPC/Flow/policy/safety/exit. Implemented behavior was not marked runtime-passed because a runner was absent.
- Native backend, Compose Wasm, typed kRPC/Flow/four files/security stayed. No wrapper/C++ host/Rust/loader DLL was restored. Cross-platform acceptance remained open.

## First migration pass: historical, before repair

- Parent verified four first-pass hashes; receipts/logs were retained under build/review/direct-native-first-pass. They cannot prove ordinary startup/other platforms given the repaired source defects.
- AppKit/WKWebView, SDK Win32/WebView2 COM and declaration-only GTK/WebKitGTK replaced wrapper code. Kotlin provided trusted runtime discovery/absolute LoadLibraryW/internal entry/module retention/callbacks.
- Removed webview.patch, webview_policy.cc, wrapper .def/headers/static archive and resources/webview. Kept assets/kRPC/capability/Origin/path/single-window boundaries and Compose routing/dialogs.
- Daemon 45062, Gradle 9.6.1, JDK /Users/stream/Library/Java/JavaVirtualMachines/graalvm-jdk-21.0.8/Contents/Home, two workers.
- prepare-native-mac.sh all prepared toolchains/locked Linux SDKs, no C++ wrapper. release-all-mac.sh all passed strict cache, final 91 tasks (9/82); repeat four links up-to-date.
- Native/resource gates and Node 4/4 passed. Policy 11/11 and repeat four links 756ms/78 tasks (6/72). Bindings used official WebView2.h 1.0.3650.58/EventToken.h and Linux declarations without prepared archives; SDKs only final-link inputs.

| First-pass target/path under build/release | Bytes | SHA-256 | Coverage |
| --- | ---: | --- | --- |
| macosArm64/Kauri | 17,010,208 | `f950f152bde46b09ce3232b0516d62eff6d3dcf9ad70efc4fd6e043efbe4f0ff` | Real GUI/runtime smoke |
| mingwX64/Kauri.exe | 19,619,840 | `985450a31bf41e8cacbca119909db804580ffe234ac6d325cbc53908a8051eab` | Cross-link/PE imports, no runner |
| linuxX64/Kauri | 18,751,344 | `1e0fff648c55e2b2a69f93f63e3ca8d9fae470352e7df84864146ab7d52e4c9d` | Cross-link/ELF, no isolated GUI |
| linuxArm64/Kauri | 18,220,208 | `84c3d4a9f946e196678ab0567388445b3e627b822b0344e623d64ada655139b6` | Cross-link/ELF, not run |

- file confirmed all formats. Mac otool showed system Foundation/AppKit/WebKit. PE imported system ole32/USER32/ADVAPI32, not WebView2Loader. llvm-readobj was unavailable in that SDK; its failed command was not ELF-dependency evidence.
- Mac copied-file offscreen Canvas 110, Native info/notes/oversize/Flow/RPC 403 and actual foreign-host navigation/popup rejection passed; services/temp data/handlers cleaned without desktop activation.
- Security seven passed; raw smoke evidence: build/review/direct-native-smoke and build/review/direct-native-security.log, no capability/credentials/notes.
- Windows/Linux had no CLI or GUI execution despite ambiguous former “CLI coverage” wording.
- Accepted internal-entry version/ABI risk, Linux system/WasmGC/display dependencies, and missing production signing/notarization/installers/updates remained. Historical static-webview reports/archives were not current dependencies or artifacts.
