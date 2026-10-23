# 验证记录

## 2026-10-22 当前交付：固定临时浏览器上下文

- 删除 `persistentBrowserData`；窗口只接收 `url`、`title`。Mac/GTK 固定非持久上下文，Windows 固定独立临时 UDF；删除持久目录路径/标识逻辑，保留必要的部分初始化、回调保活和有界清理。
- Native 业务持久化、权限与流式 Source 不变；不迁移或删除旧 profile。没有启动、RSS、磁盘 I/O 改善的实测结论，Windows 临时目录仍可能写磁盘。
- 四平台真实 release 链接、14 项强制 Native 非 GUI 测试、资源 4/4、策略 12/12，以及 Mac licenses/HTTP 安全与流式检查通过。严格配置缓存/增量复用通过。
- **GUI 未运行**，浏览器上下文隔离和关闭清理仍需自有目标 runner。Windows/Linux 只有编译、链接与依赖审计覆盖。
- 当前字节：Mac 17,043,248；Windows 19,701,760；Linux x64 18,801,552；Linux arm64 18,270,232。当前 hash、证据与审查处置见 [临时上下文记录](ephemeral-browser-context.md)，发布收据 `build/release/VALIDATION.txt`。

## 2026-10-22 历史基线：删除原生离屏能力

- 删除 `WebViewWindow.offscreen` 及三平台对应分支；删除测试专用的浏览器 frame-clock 替换，不保留隐藏替代开关。窗口只走正常显示路径，流式 Source 和服务组合边界不变。
- 四平台真实 release 链接、14 项非 GUI Native 强制重跑、资源 4/4、策略 12/12，以及 Mac licenses CLI、七项 HTTP 安全和流式传输检查通过；严格配置缓存/增量复用通过。
- **本轮 GUI 未运行**：没有独占图形 runner。发布默认不打开窗口，收据明确 NOT RUN；旧离屏截图、旧 Canvas/kRPC/Flow 结果不能作为新产物验证。移除了原来一项会打开窗口的 Cocoa 故障注入单测，生命周期图形回归仍待补齐。
- 当轮字节：Mac 17,043,248；Windows 19,704,832；Linux x64 18,801,928；Linux arm64 18,270,840。Windows/Linux 只有编译、链接与依赖审计覆盖。
- 当轮 hash、测试边界、审查处置见 [离屏消融记录](offscreen-ablation.md)；证据 `build/review/remove-offscreen/`。当前收据已被固定临时上下文产物替换。

## 2026-10-22 历史基线：流式嵌入资源

- 删除整份 ByteArray 资源接口，改为 `openEmbeddedResource(path): kotlinx.io.Source?`；框架按需最多 8 KiB/次填充 IO 段。应用用 `source.use { call.respondSource(it, type) }`，不走固定版 Ktor 会提前累积指定长度的分支。
- 四平台真实 release 链接、15 项强制重跑 Native 测试、资源 4/4、策略 11/11，以及 Mac 单文件离屏 Compose/kRPC/Flow/窗口安全与七项 HTTP 安全检查通过。
- 新回归验证背压/取消关闭，以及 8,652,729 字节真实 Wasm 的 chunked 传输、客户端中断后重新读取与字节一致性。未测总 RSS，不声称零拷贝。
- 当轮字节：Mac 17,043,248；Windows 19,704,832；Linux x64 18,804,376；Linux arm64 18,273,928。配置缓存/增量复用通过；Windows/Linux 仍只有编译、链接与依赖审计覆盖。
- 完整契约、当轮 hash、固定版官方源码依据与审查处置见 [流式实现记录](streaming-resources.md)；证据 `build/review/streaming-resources/`。当前发布收据已由离屏消融产物替换。

## 2026-10-22 历史基线：原子能力重做

- 新框架只提供嵌入资源读取和 `WebViewWindow.run/requestClose`；旧启动器、服务器包装、宿主实现已删除。应用直接拥有 Ktor/kRPC 组合根。
- 四平台真实 release 链接；Mac 单文件离屏 Compose Canvas、typed kRPC/Flow、导航/新窗口防护与七项 HTTP 安全检查通过。Native 10、资源 4、脚本策略 11 项通过。
- 清理应用输出并禁用构建缓存的恢复、配置缓存/增量复用、无 prepared 输入的导入准备通过。没有在用户工作站运行构建/主动触发 IDEA 导入。
- Windows/Linux 当前是编译、链接和依赖审计覆盖，不是 CLI/GUI 运行通过。Cocoa 故障清理测试使用故障注入，不冒充真实网络错误时序验证。
- 当轮字节：Mac 17,043,232；Windows 19,693,568；Linux x64 18,793,032；Linux arm64 18,262,352。当轮二进制与源码 manifest 已同步并校验。
- 当轮 SHA-256、完整验证边界及独立审查处置见 [原子重做记录](atomic-rewrite.md)；原始证据 `build/review/atomic-rewrite/`。当前发布收据已由流式实现替换。

## 以下为历史记录，不能代替当前产物验收

日期：2026-10-21。所有应用/宿主编译在 `stream@macbook:~/ACodeSpace/demo/Kauri` 完成；没有在 Linux 工作站运行 Gradle。旧的 static-webview 探针和二进制不作为本次迁移证据。

## 修复基线：源码阻塞已修复，跨平台运行验收待补齐

- Session 1034 在原迁移方向内修复；没有重新选型。Session 1029 确实是首轮实现的受委派 subagent，其另一次 agy review 额度失败不等于未委派。
- 普通启动仅在 smoke 创建 `Smoke`；标题按应用配置传递。Windows 内部入口改为已核对的五参数 ABI，并补 COM 所有权、事件注销、resize、异步/构造失败清理及安全卸载。Linux 补 policy 0/1/2、真实 JavaScript 执行和 smoke-only 消息桥。Mac 窗口 delegate 停事件循环，不再 `terminate` 跳过 `finally`。
- [逐项复核及主来源](direct-native-review.md) 已更新。Windows/Linux 尚无隔离 GUI runner；任务保留在 `kanban/executable/`，不是全平台运行验收完成。

## 修复后构建与回归

- 所有应用/宿主构建仍在 Mac；`scripts/gradle-mac.sh` 检测并明确复用 daemon `45062`、GraalVM JDK 21.0.8，2 workers。未触发本机 Gradle/import、未关闭用户 IDE/无关 daemon；同步前逐文件比较，未 blanket delete。
- 首次修复 release 四个目标均真实重新链接：55s，`91 actionable tasks: 17 executed, 74 up-to-date`；Windows/Linux 细化后再次链接。最终交付 release：31s，`91 actionable tasks: 15 executed, 76 up-to-date`，严格配置缓存复用。上述耗时不是平台性能比较。
- 最终同一组七个 qualified task 请求重复成功：641ms，`91 actionable tasks: 6 executed, 85 up-to-date`，四个 release link 均 up-to-date，严格配置缓存复用。日志为 `incremental-delivery.log`。
- GTK 声明头变更曾暴露 cinterop 的旧 up-to-date 结果；给两个现有 cinterop task 添加仓库头文件输入后重新生成，两 Linux 编译和链接通过。仍不依赖 prepared archive/SDK 来生成 IDE bindings；未新增 root aggregate、自定义 task class 或 build-logic。
- 有界 producer 恢复检查：仅移走本任务检查的 Mac `frontend.o` 并保留回退副本，请求 release link 自动调度 `embedMacosArm64Frontend`，恢复字节与副本完全一致，四个 link 保持 up-to-date；配置缓存复用，581ms。副本已移除，没有清空应用/SDK/历史工作目录。
- 当前 Native 测试通过：framework 5 项（含新增 C 函数指针五参数转发和 GTK policy 0/1/2 合约），shared 2 项，Native service/storage 1 项。除 Gradle 门禁外，8 项测试还在 Mac 直接运行现有 Native 测试二进制全部通过；不是拿首轮收据代替重跑。
- Node 资源测试 4/4、构建策略/锁/失败诊断/有界子进程清理 11/11 通过。`--licenses` 在 Mac 执行成功；其他平台没有执行 CLI。
- ABI 补充证据：Mac C 函数指针测试检查布尔/enum/UTF-16/pointer 宽度及真实转发；Apple clang 21 对 `x86_64-w64-windows-gnu` 的编译期 static assertions 和 IR 证实 `bool8 / enum32 / wchar16 / HRESULT32 / pointer64` 及五参数调用形状。这是声明/编译证据，不是 Windows 内部入口实际运行通过，也没有加入 C/C++ 宿主。

## 本轮宿主消融

- [删除决策与独立复核](native-host-ablation.md)：四个宿主文件 1,236 → 1,139 行，净减 97 行；删除重复状态、handler 标签映射和分配接线，保留实际生命周期及安全边界。
- 本轮四目标重新链接通过；Mac 普通离屏启动/关闭/finally、真实 Canvas/kRPC/Flow/policy 和七项 HTTP 安全检查再次通过。8 项 Native 测试重新执行，资源 4/4、策略 11/11 通过。
- release 构建 54s，91 tasks（17 executed / 74 up-to-date）；重复同组任务 575ms，四个 link up-to-date、严格配置缓存复用。没有据此宣称性能改善。
- 证据在 `build/review/host-ablation/`；上节及下文 Session 1034 的 repair 日志仍是该轮历史证据。当前收据/hash 已更新为消融产物，旧修复收据另存，未冒充本轮结果。
- 构建仍复用 Mac daemon 45062/JDK 21.0.8、两个 workers，未运行本机 Gradle 或 IDEA import；Windows/Linux GUI 运行缺口不变。

## 本轮应用接入 API 消融

- [公开 API 消融记录](public-api-ablation.md)：删除 `DesktopApplication`、公开服务器工厂和 Route/path 回调；应用仅调用一个非 inline 的 typed 启动函数，服务构造函数每次启动执行一次。底层窗口/安全/生命周期行为不改。
- 四目标重新链接通过，56s / 91 tasks（13 executed / 78 up-to-date）；重复 608ms / 91 tasks（6 executed / 85 up-to-date），严格配置缓存复用，四个 link up-to-date。
- Native 8 项、资源 4/4、策略 11/11，以及 Mac 普通离屏启动/关闭、真实 Canvas/kRPC/Flow、防护、七项 HTTP 安全和 `--licenses` 重新通过。
- 应用只依赖 framework 和共享契约即可编译，不导入 Ktor server。负例编译准确拒绝实现类注册；framework 单测无须链接应用资源。
- 本轮在 Mac 无 daemon 时以显式 GraalVM JDK 21.0.8 初始化任务专用 daemon 84337，之后始终复用，两个 workers；本机未运行 Gradle/import。证据在 `build/review/public-api/`，旧收据另存；Windows/Linux 运行缺口不变。
- 收尾时确认该任务 daemon 空闲、JVM/PID/最后项目均匹配后，仅停止 84337；未停止无关 daemon。临时探针已移出应用源码，传输/解压副本和锁已清理。

## 历史 API 消融后四产物（当轮交付）

本地与 Mac 的 manifest/收据一致，下载后再次 `sha256sum -c SHA256SUMS` 四项通过；`VALIDATION.txt` 已替换为本轮检查及当前 hash。

| 目标 | 文件 | 字节 | SHA-256 | 实际覆盖 |
| --- | --- | ---: | --- | --- |
| macosArm64 | `build/release/macosArm64/Kauri` | 17,010,288 | `29e35cdd854e929417e9af39f364727f02548aa944b3a827f138bc1f328ea663` | 原生链接、CLI、离屏普通初始化/关闭、真实 GUI smoke/security |
| mingwX64 | `build/release/mingwX64/Kauri.exe` | 19,620,352 | `66cbe61268a2abf91aa47a02b221f1f7f929c5edd88a5759f6f2574598aa8340` | 交叉链接/PE 格式；未运行 CLI 或 GUI |
| linuxX64 | `build/release/linuxX64/Kauri` | 18,774,120 | `e3d9c335e47f15673a6bde6e8d06be8f07cd01ef81fe812930333918a1f6dbdd` | 交叉链接/ELF 格式；未运行 CLI 或 GUI |
| linuxArm64 | `build/release/linuxArm64/Kauri` | 18,247,016 | `2e372845c89abd7fa542eda76871ff957bbc3d2ce07da82ea5dd61d1159b6ae9` | 交叉链接/ELF 格式；未运行 CLI 或 GUI |

## 修复后 Mac 运行证据与缺口

- 单文件从空的自有临时目录运行，未携带资源目录；所有测试窗口屏外、无 `activate`/桌面接管。
- 普通回归命令只有 `--startup-check`，不传 `--smoke`/`--result-dir`。保留普通 URL、WebKit 默认 profile、无 `Smoke` 状态；只隔离应用数据、屏外放置，并在真实文档加载后调用 `performClose` 走窗口 delegate。日志含 `KAURI_STARTUP PASS`，进程 exit 0；这不是人工点击用户桌面测试。
- 真实 smoke：Canvas 110 采样色、Native systemInfo、Unicode note round-trip、超长输入拒绝、Flow 1/2/3、未授权 RPC 403、foreign navigation/new-window 拒绝，`policyVerified=true`。
- 普通回归与 smoke 都自然退出。smoke 日志含 `KAURI_CLEANUP PASS`，位于宿主关闭、server.stop 和临时数据清理后的 common `finally` 之外；Mac 临时目录删除失败现在会抛错，不会打印成功。未通过进程 terminate 冒充有序关闭。
- HTTP 七项：health 200；foreign Origin、foreign Host、missing WebSocket Origin、invalid capability 为 403；encoded/raw traversal 为 404。
- 证据在 `build/review/direct-native-repair/`：`release-delivery.log`、`smoke/{startup.log,process.log,result.txt,window.png}`、`tests-*.log`、`policy.log`、`incremental-delivery.log`、`producer-recovery.log`、ABI probe IR。当前 hash 收据在 `build/release/{VALIDATION.txt,SHA256SUMS}`。不保存 capability URL 或机器凭据。
- Windows 仍待隔离 runner 验证真实 Runtime/内部入口、COM 引用/token/晚到回调、resize、普通关闭和完整 Compose smoke/security。若回调未在 5s 内释放或 DLL 不允许卸载，源码会安全保留模块至进程结束；不能把这当作所有 Runtime 版本都无泄漏的证明。
- Linux x64/arm64 仍待自有隔离 display 验证响应加载、evaluate/message bridge、WasmGC/Canvas/kRPC/Flow/policy/security 与普通/smoke 退出。功能已实现，但没有把缺 runner 写成运行通过。
- 最终仍保留 Native backend、Compose Wasm、typed kRPC/Flow、四个单文件和既有安全边界；没有重新加入 wrapper/C/C++ 宿主/Rust/loader DLL。源码及文档复核完成；跨平台运行门禁和用户验收尚未完成。

---

## 首轮迁移记录（历史，以下不是修复后证据）

- 父会话曾独立复核首轮四产物 hash；下表和首轮日志保留审计用途。当前产物和收据已由本轮替换，首轮 manifest/receipt 副本另存 `build/review/direct-native-first-pass/`。
- 首轮存在本报告顶部所述源码缺陷，首轮 smoke 不能证明普通启动或其他平台功能正确。

### 首轮迁移结果

- macOS：`framework/src/macosArm64Main/.../DesktopHost.kt` 直接调用 AppKit/WKWebView。
- Windows：`framework/src/mingwMain/.../DesktopHost.kt` 直接调用 Win32 和官方 WebView2 SDK cinterop；Kotlin 实现受信任路径发现、绝对路径 `LoadLibraryW`、内部入口 `CreateWebViewEnvironmentWithOptionsInternal`、模块保活和 WebView2 COM 回调。
- Linux：`framework/src/linuxMain/.../DesktopHost.kt` 直接调用 GTK/WebKitGTK；`nativeInterop/linux/kauri_gtk.h` 仅为声明兼容头，没有 C/C++ 宿主实现。
- 已删除 `framework/src/nativeInterop/webview.patch`、`webview_policy.cc`、旧 wrapper cinterop 定义/头文件、静态 archive 生成和 `resources/webview` 子模块。
- 保留 Compose Wasm、资源 object 嵌入、typed kRPC/Flow、loopback capability/Origin/静态路径安全和单窗口边界。导航与内容内对话框仍由 Compose Wasm 负责。

### 首轮构建与复用

- Gradle 9.6.1 daemon PID `45062`，JVM `/Users/stream/Library/Java/JavaVirtualMachines/graalvm-jdk-21.0.8/Contents/Home`，`scripts/gradle-mac.sh` 复用该 daemon，2 workers。
- `bash scripts/prepare-native-mac.sh all` 成功：准备 Kotlin/Native 工具链和锁定的 Linux GTK/WebKitGTK link inputs；不编译 C/C++ wrapper。
- `bash scripts/release-all-mac.sh all` 成功；配置缓存严格模式通过，最终一次构建 `91 actionable tasks: 9 executed, 82 up-to-date`。重复 release-link 请求复用配置缓存并保持四目标 link up-to-date。
- `scripts/test-build-policy.sh`、Native 测试、资源嵌入测试均作为 release 脚本门禁；本次 release 输出中 Node 资源测试 4/4 通过，三个 macOS Native test 任务成功。
- 独立 `scripts/test-build-policy.sh` 11/11 通过（worker/JVM/路由/锁/失败诊断/有界子进程清理）；重复四目标 link 请求在 756ms 内完成，`78 actionable tasks: 6 executed, 72 up-to-date`。
- WebView2 cinterop 使用仓库内官方 SDK `WebView2.h`（1.0.3650.58）和兼容声明 `EventToken.h`；不依赖 Mac prepared archive。Linux cinterop 也只依赖声明头，link 阶段才跟踪 SDK 目录。

### 首轮四个 release 产物

以下是首轮大小/hash；不是当前 `build/release/` 的内容。

| 目标 | 文件 | 字节 | SHA-256 | 覆盖 |
| --- | --- | ---: | --- | --- |
| macosArm64 | `build/release/macosArm64/Kauri` | 17,010,208 | `f950f152bde46b09ce3232b0516d62eff6d3dcf9ad70efc4fd6e043efbe4f0ff` | 真实 GUI/runtime smoke |
| mingwX64 | `build/release/mingwX64/Kauri.exe` | 19,619,840 | `985450a31bf41e8cacbca119909db804580ffe234ac6d325cbc53908a8051eab` | Windows release link/PE import cross coverage；没有 Windows runner |
| linuxX64 | `build/release/linuxX64/Kauri` | 18,751,344 | `1e0fff648c55e2b2a69f93f63e3ca8d9fae470352e7df84864146ab7d52e4c9d` | Linux x64 release link/ELF cross coverage；没有隔离 GUI runner |
| linuxArm64 | `build/release/linuxArm64/Kauri` | 18,220,208 | `84c3d4a9f946e196678ab0567388445b3e627b822b0344e623d64ada655139b6` | Linux arm64 release link/ELF cross coverage；未运行 |

远程 Mac 的 `file` 检查确认 Mach-O arm64、PE32+ x86-64、ELF x86-64 和 ELF aarch64。Mac `otool -L` 只显示系统 `Foundation/AppKit/WebKit` 等依赖；PE 导入含 `ole32.dll`、`USER32.dll`、`ADVAPI32.dll` 等系统 DLL，不含 `WebView2Loader.dll` 或 wrapper archive。跨格式 `llvm-readobj` 在本机 SDK 不可用，没有用失败的命令冒充 ELF 依赖证明。

### 首轮实际运行覆盖

- Mac 单文件离屏 smoke 通过：二进制复制到临时空目录，从屏外窗口开始，Compose Canvas 采样 110 色，Native `systemInfo`、Unicode note round-trip、超长输入拒绝、Flow 1/2/3、未授权 RPC 403；结果为 `policyVerified=true`。
- Mac smoke 真实触发 foreign-host 文档导航和新窗口请求，WKNavigationDelegate/WKUIDelegate 拒绝；关闭时清理服务、临时目录和 WebView handler，未激活用户桌面。
- Mac security smoke 通过：health 200；foreign Origin/Host、缺 WebSocket Origin、错误 capability 为 403；encoded/raw traversal 为 404。
- `build/review/direct-native-smoke/` 和 `build/review/direct-native-security.log` 保存本次 smoke 输出；不保存 capability URL、机器凭据或启动临时笔记。
- Windows/Linux 首轮只有交叉编译/文件格式覆盖，没有实际执行 CLI 或 GUI；原记录的 “CLI coverage” 不应理解为在目标系统运行过。实际 Windows Runtime/COM 生命周期仍需隔离 Windows runner。

### 首轮残留风险（当前运行缺口见上文）

- Windows 内部入口是用户明确接受的版本兼容风险；运行时更新可能改变入口或其未公开 ABI。实现不切换为 Rust 或官方 loader DLL。
- Linux 普通构建直接使用系统 GTK/WebKitGTK，且当前跨链依赖目标系统提供 WasmGC 能力；没有 Linux GUI runner 时，窗口、Wasm 文档和退出流程尚未实测。
- Mac smoke 证明了本次直接宿主和应用链路，但不等同于 Windows/Linux runtime 认证、正式签名/公证、安装器或自动更新。
- 旧 `resources/static-webview-spike.md`、`kanban/done/*static-webview*` 是历史证据；不得把其中的 wrapper archive 当作当前产物或依赖。
