# 原子能力重做记录

日期：2026-10-22。用户确认两能力规格后要求清空旧代码重做；本文记录实现和证据边界，最终产物完整性以同批 `build/release/SHA256SUMS` 和 `VALIDATION.txt` 为准。

后续变更：用户已拒绝下文记录的整文件 ByteArray 读取，资源 API 改为流式 `openEmbeddedResource`。当前实现/产物见 `resources/streaming-resources.md`；本页的 API 名称与 hash 是原子重做基线，不代表流式版本。

## 实现边界

- 删除旧框架全部 Kotlin 宿主、`LocalServer`、`EmbeddedAssets`、配置/平台辅助与总启动器，再从规格编写新文件；不保留旧入口兼容层。
- 保留应用 DTO、业务存储与 Compose 内容、官方 SDK 声明、资源打包格式及必要工具链。这不是删除业务后做一个空窗口。
- 先前已接触旧实现，因此这里的“重做”不是形式化未接触源码的 IP 净室证明。
- 公开 API 仅 `readEmbeddedResource`、`WebViewWindow` 构造及 `run` / `requestClose`；窗口不创建服务器、调用服务工厂、管理业务目录或退出进程。

## 新代码职责

| 文件 | 职责 |
| --- | --- |
| `framework/.../WebViewWindow.kt` | URL/标题校验、单次运行与单活动窗口、原子关闭请求 |
| `framework/.../EmbeddedResource.kt` | 校验 KAUR 包、按名称返回独立字节副本；不附带 HTTP 层 |
| `framework/.../Window.macos.kt` | AppKit/WKWebView、数据上下文、策略与窗口消息循环 |
| `framework/.../Window.windows.kt` | Win32、SDK typed COM callbacks、五参数内部入口、有界 profile 清理 |
| `framework/.../Window.linux.kt` | GTK/WebKitGTK、信号/源/引用所有权、消息循环 |
| `app/desktop/.../Main.kt` | 应用组合根：Ktor、typed kRPC、HTTP 安全、资源路由和 CLI/测试生命周期 |

- `run()` 内创建和清理 native 窗口；`requestClose()` 只存原子标记，平台 UI timer 检查它。不把 HWND/ObjC/GTK 指针跨线程交给调用者。
- 自然关闭与请求关闭都结束消息循环，随后由应用 `finally` 停止 Ktor/协程。Cocoa 故障、Windows 回调故障和 GTK 失败信号保存为内部错误，在受控退出后抛出。
- 初始 URL 要显式 loopback 端口；导航校验接受浏览器省略 HTTP 默认端口的标准化形式。单元测试覆盖默认端口、前导零和伪 authority。
- 暂不提供后续导航、脚本执行、消息桥、Ready/CloseReason 流、Profile 管理器或应用 DSL。业务操作与 Flow 仍走 typed kRPC，测试 HTTP 端点不替代业务 RPC。

## 删除的旧层与构建输入

- 框架不再依赖 Ktor、kRPC、serialization 或 coroutines；这些归应用自己的组合代码。
- 不维护服务器包装、公开服务工厂协议、平台配置对象、Smoke 状态机或第二套 dispose/awaitClosed 协议。
- 嵌入资源头只声明 start/end 数组；数据跨度与解析均在 Kotlin 中完成，无手写 C 函数。cinterop 自动生成的桥仍属于 Kotlin/Native 工具链。
- Linux 只保留实际使用的声明，删除脚本消息/evaluate 绑定，以及构建脚本中不再直接使用的 JSC/GDK/GIO/dl 显式链接声明；不代表浏览器或 Native 运行时不再加载这些系统依赖。
- 资源 cinterop 用普通 target 循环配置，资源与 SDK 头文件是显式任务输入。未引入 build-logic、自定义任务类或根聚合。
- Windows 加载路径只使用已发现的运行时目录及系统依赖搜索；模块按绝对路径缓存到进程结束。删除卸载协议，避免最后一次 `Release` 返回 DLL 栈时的卸载竞态；COM 对象/事件仍正常释放。

## 验证范围

- 首轮完整发布已完成四个真实 release 链接；Mac 独立文件复制至空目录后运行。
- Mac：非 smoke 应用路径初始化/请求关闭，真实 Compose Canvas（最终 130 sampled colors）、Native 系统信息、Unicode 笔记、超长写入拒绝、Flow 1/2/3；原生导航与新窗口拒绝；七项后端 HTTP 安全检查通过。
- 测试：框架 6、应用存储/路径 2、共享契约 2，共 10 项 Native；三个测试任务另用 `--rerun-tasks` 强制执行。资源打包 4 项、脚本策略 11 项通过，未使用旧测试 XML 冒充新测试。
- 清理四个 release 输出、嵌入包/object 和前端 distribution 后，禁用构建缓存恢复：production distribution、pack、四个 embed 和四个真实 link 均执行，57 秒成功。复用验证命中配置缓存，四个 link 均 UP-TO-DATE；最终一轮 Gradle 报 564ms。
- 无 prepared 输入：临时移走整个 `build/native-deps`，强制执行 framework/app 的 `prepareKotlinIdeaImport`，36 项任务执行成功，随后恢复目录。这是远程导入准备验证，不是用户本机 IDEA 实际同步。
- Windows/Linux：本批先按交叉编译/链接记录，不据此声称 GUI 或 CLI 已运行。工作站存在用户 IDE/compiler 活动，本轮不争用它启动浏览器/虚拟机来补覆盖。
- 跨机源文件校验发现远端 notices 仍描述历史静态 wrapper，已换成本地正确声明并重新打包四目标、重跑 Mac 检查；`--licenses` 实跑确认新说明来自嵌入资源，而不是旁置文件。

### 最终产物

| 目标 | 字节 | MiB | 运行覆盖 |
| --- | ---: | ---: | --- |
| macosArm64 | 17,043,232 | 16.25 | 单文件离屏 UI / RPC / 安全 |
| mingwX64 | 19,693,568 | 18.78 | 编译、链接和 PE 导入表 |
| linuxX64 | 18,793,032 | 17.92 | 编译、链接和 ELF 依赖 |
| linuxArm64 | 18,262,352 | 17.42 | 编译、链接和 ELF 依赖 |

- 路径为 `build/release/<target>/Kauri`，Windows 为 `Kauri.exe`。每个文件可独立复制，但仍需对应系统 WebView/GUI 运行库。
- SHA-256：Mac `505a83ddda1f96c044352e393c8173ff817d5148b681dcab280f7747bd02f722`；Windows `c5a7c1025b9c2e06d0b7cb09e3572aa024089a1f35c0be1ae63712e70a5ff7ce`；Linux x64 `3f39dd506643aee75604ba89a6b3f74b3d3cafce8dbf917c4490c090b96b03d0`；Linux arm64 `6820a68277975590c9c114066e89dc38ba9adc4d991ae48d2bf99c3d1effc5c9`。
- Mach-O/PE/ELF 架构检查匹配目标；Windows 导入表只有系统 DLL，没有 loader、JVM 或 libstdc++ companion DLL。Linux 直接依赖 WebKitGTK/GTK/GObject/GLib 和系统运行库；删除显式 link 选项不代表 Native 运行时不再依赖 libdl 等库。
- 二进制、源码 manifest、截图、测试 XML 和原始日志同步到本机后再校验；源码收据为 `build/review/atomic-rewrite/SOURCE-SHA256SUMS`，二进制以发布 manifest/receipt 为准。

### 故障测试的区别

- 真实 WKWebView 运行中，通过公开 Cocoa SDK delegate 接口注入初始导航错误，验证错误保存、关闭消息循环、清理与后续另一窗口再次运行；不增加产品测试 API。
- 最初用不监听的 loopback socket 期待浏览器迅速失败；实际在 20 秒内仍处于加载中，不能把任意等待时间当作错误事件保证。最终用可控故障注入测试清理链，外部测试 deadline 只负责防止测试悬挂。
- 这只覆盖 delegate 收到错误后的清理退出，不证明真实无响应/慢网络下的底层超时触发或所有初始化失败。窗口没有内置固定导航超时；调用者自行安排后台失败监视/超时并请求关闭。
- Native 构建失败、早期失败测试与修复日志均保留在忽略目录 `build/review/atomic-rewrite/`，不以旧产物冒充重做验证。

## 独立审查与处置

- agy 的两个有工具实现尝试最终均确认零文件修改；本次最终源码由主会话编写。Flash Medium 审查没有返回正文，不能记为完成独立审查。
- Flash Low 返回 Windows 与 core 两份新源码审查；原始结果为 `agy-windows-low.json` / `agy-core-low.json`。逐项核实，不照单接受其“Proof”标签。
- 保留/修正：HTTP 默认端口标准化；强制销毁前清空 HWND user data 并检查销毁结果；删掉临时文件清理中不必要的属性重置。后者避免硬链接共享属性的副作用，不等于确认“删除链接会删除外部目标”的报告说法。
- 拒绝增加层次：把窗口句柄换成获取回调、给只在主线程串行访问的 runtime map 加并发容器、再加 Profile 或事件协议。
- 拒绝错误告警：已做 Long 上界验证的 Int 偏移不会在更新时越过 `Int.MAX_VALUE`；COM 零引用释放不允许事后重获无效引用；GTK 显式持有的引用不能在 destroy 回调中简单置空，否则泄漏；默认 policy 返回 FALSE 允许默认处理；没有 ARM32 构建目标。
- 文档另经 Flash Low 复核：明确显式 link 声明、Windows 清理限制与 Cocoa 故障注入范围，合并重复的性能免责声明；无阻断性文档问题。

## 官方来源与限制

检索日期：2026-10-22。以下为判断依据，不是跨平台运行证明。

- [IUnknown 方法顺序](https://learn.microsoft.com/en-us/windows/win32/api/unknwn/nn-unknwn-iunknown)与[零引用 Release](https://learn.microsoft.com/en-us/windows/win32/api/unknwn/nf-unknwn-iunknown-release)：支持 SDK vtable 前缀接线和实际引用所有权，不支持“归零后还允许另一客户端 AddRef”的假设。
- [DestroyWindow](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-destroywindow)：同步发送销毁消息，调用者必须处理失败；[线程访问权限](https://learn.microsoft.com/en-us/windows/win32/procthread/thread-security-and-access-rights)中 `0x0800` 是 QUERY_LIMITED_INFORMATION，不是审查所称 QUERY_INFORMATION。
- [硬链接属性](https://learn.microsoft.com/en-ca/windows/win32/fileio/hard-links-and-junctions)、[DeleteFileW](https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-deletefilew)：删除链接和改变共享文件属性是不同操作。清理不递归进入 reparse point，也不重置属性。
- [内部环境创建入口](https://learn.microsoft.com/en-us/microsoft-edge/webview2/reference/win32/webview2-idl?preserve-view=true&view=webview2-1.0.2420.47)记录 bool/int/UTF16/options/handler 五参数，并明确可能修改或移除。用户已接受此兼容性风险。
- [Kotlin 2.4 UUID random](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.uuid/-uuid/-companion/random.html)列出 Native 平台 CSPRNG。应用拼接两个 v4 UUID 作会话 capability（有效随机位至多 244），不是把 UUID 当作加密密钥或宣称 256 位熵。
- [GTK ref_sink](https://docs.gtk.org/gobject/method.Object.ref_sink.html)明确取得普通强引用；[GtkWindow 初始引用](https://docs.gtk.org/gtk3/ctor.Window.new.html)由 GTK 持有。[WebKitGTK 默认 policy](https://webkitgtk.org/reference/webkit2gtk/stable/signal.WebView.decide-policy.html)在未处理时执行默认 use。它们支持源码中的持有/释放和 FALSE 分支，不支持审查建议直接置空自有引用。

## 未覆盖项

- Windows Runtime 内部入口、真实 COM/resize/晚到创建与临时 profile 删除仍需要 Windows 10/11 runner。代码有界等待和重试，失败报错并保留必要状态，不保证 Edge 尚占用目录时仍能删除，也不把失败静默记为已清理。Linux 两架构仍需要隔离图形会话验证 GTK、WasmGC、绘制和关闭。
- Mac 测试只用非持久浏览器数据，不以保护用户数据为代价验证普通持久 profile；Windows 新的按可执行文件身份选择目录不承诺迁移旧目录。
- 无性能对比，也不承诺所有系统 WebView 版本兼容、二进制可复现或无需系统浏览器运行库。
