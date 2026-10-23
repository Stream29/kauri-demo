# 纯 Kotlin/Native 宿主可行性探索

日期：2026-10-21。基于 Kotlin 2.4.0、WebView2 SDK 1.0.3650.58 和迁移前固定的 webview 0.12.0；本文是 ABI 可行性证据，不是当前 wrapper 依赖。

## 结论与范围

- **以“不维护手写 C/C++ 实现”为目标，可行性已得到关键 ABI 探针支持**；尚未完成 Windows/Linux 宿主迁移或完整 GUI 验证。
- macOS 窗口宿主已经直接使用 Kotlin/Native。Linux 可以直接调用系统动态库；Windows 可以使用 SDK 生成的 C COM 接口绑定。
- Windows 的主要选择点是 loader：Kotlin 可以承接现有加载逻辑，但这条路线依赖内部运行时入口，不等于只调用微软公开 API。
- “纯 Kotlin/Native”仍允许 SDK 声明头、cinterop 自动生成的桥接代码、Kotlin/Native 运行时及系统动态库；不表示最终运行的所有机器码都由 Kotlin 编写。
- 本轮没有修改生产源码、Gradle 配置、发布二进制或架构决策，也没有激活 GUI 窗口。

## 实际验证

探针在 Mac 上串行编译，使用已有 Kotlin/Native CLI、显式 GraalVM 21.0.8、项目锁及 512 MiB–1 GiB 的 JVM 上限；没有运行 Gradle。

| 部分 | 实际结果 | 未证明的内容 |
| --- | --- | --- |
| Linux x64 | Kotlin 探针编译；本机低优先级执行通过 GLib 动态函数调用、Kotlin 回调、`StableRef` 状态传递及 destroy notify 释放；解析到 GTK/WebKitGTK 创建函数 | 没有初始化 GTK 或创建 WebView，未验证 GUI、渲染或导航策略 |
| Linux arm64 | 相同源码交叉编译为 aarch64 ELF | 未在 arm64 运行 |
| Windows COM | 官方 `WebView2.h` 的 C 接口成功生成 klib；纯 Kotlin 探针链接为 PE x64，覆盖 SDK vtable、回调、Win32 WNDPROC、COM 方法调用及 `RECT` 按值参数 | 没在 Windows 执行；没有建立真实 COM 环境、创建 WebView 或调用实际 loader |
| 资源链接符号 | Mac 探针通过仅含 `extern` 数组声明的头文件读取汇编链接的数据，实际运行验证 4 字节内容和起止指针差值 | 仅验证接口替代方式，没有修改或重新打包生产资源 |

- Windows 初次编译失败是 GUID 类型的命名空间问题；检查生成元数据后使用 `platform.posix._GUID`，第二次编译通过，不需要手写 GUID 布局。
- Windows SDK 探针复用了 `EventToken.h` 小型兼容头：它只有结构体声明，不是 C 实现；仍需处理 SDK/MinGW 声明差异。
- COM 探针只验证类型和链接路径；其中简化的 `QueryInterface`、引用计数和模拟调用不是可交付的 COM 对象实现。
- 探针不依赖现有 `host.o`、`loader.o` 或预编译 WebView2 loader。Windows 导入表检查未出现 WebView2Loader DLL，但这不证明实际环境创建已完成。
- 证据与源码快照归档在忽略目录 `build/review/pure-native-*`；本机和 Mac 的临时 klib、程序及编译目录均已移除。

## Windows loader：语言问题与 API 边界不同

微软的公开部署路径是随应用提供 `WebView2Loader.dll`，或静态链接 loader。见 [部署说明](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/distribution) 与 [单可执行文件说明](https://learn.microsoft.com/en-us/microsoft-edge/webview2/how-to/static)。

当前固定版本的 [webview loader 源码](https://github.com/webview/webview/blob/0.12.0/core/include/webview/webview.h#L3288-L3517) 会尝试 loader DLL，再通过注册表查找安装目录，加载 `EmbeddedBrowserWebView.dll` 并解析 `CreateWebViewEnvironmentWithOptionsInternal`。

| 路线 | 能否不维护 C/C++ 实现 | 代价 |
| --- | --- | --- |
| Kotlin 调官方 loader DLL | 可以 | 仍有原生 loader 文件；旁置 DLL 不满足当前独立复制单文件的行为。嵌入后释放 DLL 属于另一种交付设计，未探索或采纳 |
| Kotlin 链接官方静态 loader | 可以不维护自己的 loader 源码 | 仍依赖预编译原生库；之前的 MinGW/MSVC 链接问题本轮未重新解决 |
| Kotlin 实现当前内置 loader 的必要部分 | 关键函数指针与 Win32 能力支持此方向，但本轮未完成 loader | 保留内部入口、注册表布局、版本与模块生命周期的兼容责任；需要 Windows 实机验证 |

- 第三条路线可望同时保留单文件和去除手写 C/C++，但**未找到可据此承诺“零 loader 原生依赖且只用官方公开初始化入口”的证据**。
- 内部入口风险不是 Kotlin 移植才引入的，当前源码 loader 已经如此；移植只是把实现和维护责任移到 Kotlin。
- 不应猜测 COM vtable 或内部函数签名。公共接口从 SDK 生成；内部加载部分需明确版本兼容范围、失败诊断和受信任的库路径。

## 迁移后可以删什么

- Linux 的 `kauri_linux.c`，Windows 的 `kauri_windows.c` 与 `kauri_loader.cc` 可作为候选迁移对象；相应手动 C/C++ 编译任务和 object 产物接线可以随之删除。
- 如果 Windows loader 也迁移且不再调用 webview 代码，才可评估移除该源码依赖；若参考移植其实现，不能因此自动删除来源和许可证说明。
- 资源头里的手写 inline 取址函数可改为声明链接器数组符号，由 cinterop 生成访问代码；资源仍可用汇编嵌入。
- **仍保留** Compose Wasm 构建、资源 archive/嵌入、最终 Native 链接、系统运行依赖，以及需要的 SDK 绑定与发布签名。去掉 C/C++ 不等于没有 LLVM 或 cinterop。
- 尚未测量迁移后的构建时间、体积或维护成本。生成较大 SDK 绑定也有成本，不宣称迁移必然更快或更小。

## 实施前必须验证

- Linux：固定一小组经官方 API 核对的动态函数签名；宏不能 `dlsym`，使用实际导出函数，例如当前 C 适配层已用的 `g_signal_connect_data`。保留导航、弹窗、关闭顺序及异步回调释放，两个架构分别验证。
- Windows：为当前五类完成/事件回调实现 `QueryInterface`、引用计数和存活规则，包括最后一次 `Release` 时释放实例及 `StableRef`。禁止异常穿过 C ABI；保证 STA/UI 消息泵及非重入关闭。见 [微软线程模型](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/threading-model)。
- Loader：验证每用户/全机安装、缺运行时、安装版本变化、绝对 DLL 路径及异步请求期间的模块保活；先验证初始化，再谈删除现有实现。
- 端到端：迁移后重做单文件离屏渲染、typed kRPC/Flow、安全检查和反复启动/关闭；编译通过不能替代目标系统运行。

## 互操作依据

- Kotlin 官方 [C interop](https://kotlinlang.org/docs/native-c-interop.html) 和 [函数指针教程](https://kotlinlang.org/docs/mapping-function-pointers-from-c.html) 支持函数指针、非捕获 `staticCFunction` 回调与 `StableRef` 用户数据管理。
- GLib 的 [timeout_add_full](https://docs.gtk.org/glib/func.timeout_add_full.html) 明确规定回调停止与 destroy notify 语义；本轮运行探针验证了对应的 Kotlin 回调释放路径。
- WebKitGTK 的 [evaluate_javascript](https://webkitgtk.org/reference/webkit2gtk/stable/method.WebView.evaluate_javascript.html) 是异步 C API，调用完成后通过 finish API 获取结果；Kotlin 移植仍需保持该生命周期。

## 独立复核

- agy / Gemini 3.8 Flash Medium 只读复核了探针与本报告，确认编译/运行边界和现有 loader 风险的区分；采纳了声明头、COM 释放和 GObject 宏的说明建议。
- 未扩大到报告提出的 Windows arm64：用户目标是 mingwX64。也不为整个 SDK 的数十种事件建设通用 COM 框架，只覆盖当前场景。
- 原始复核位于忽略目录 `build/review/agy-pure-native-feasibility.json`；它是源码审查，不是目标系统运行证据。
