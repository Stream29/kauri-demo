# 原子 API：消融后的设计草案

状态：2026-10-22，用户确认后已清空旧框架实现并完成重做，随后将资源改为标准 Source、删除原生离屏和浏览器持久性选项；本轮状态见 [临时浏览器上下文](ephemeral-browser-context.md)。此前重做/流式/离屏记录是历史验证基线；本文的规格不代替运行证据。

## 判断标准

- 删掉一个 API 后，若能直接使用 Kotlin、Ktor 或 kRPC 完成同样的工作，就不另造包装。
- 必须保留的能力，与能力的唯一命名/编码方式不是一回事。“不可或缺”指满足当前单窗口、单文件及非侵扰验证目标所需，不指这个类名在理论上不可替代。
- 公开项少不等于层次少。业务创建、参数解析、服务器与窗口的启动顺序、失败处理及关闭顺序都交给应用；跨平台 ABI 和原生对象所有权留在能力内部。

## 已确认接口

```kotlin
fun openEmbeddedResource(path: String): kotlinx.io.Source?

class WebViewWindow(
    url: String,
    title: String,
) {
    fun run()
    fun requestClose()
}
```

- 两种独立能力，不引入 Application、Scope、Builder、Options、服务工厂或统一资源基类。
- 窗口构造只校验和保存参数；原生资源在 `run()` 中创建，并在它返回或抛出前完成有序清理。`requestClose()` 是请求，`run()` 返回才是清理完成的边界。
- 没有 Kauri 服务器 API：调用者直接使用 Ktor/kRPC。下面分别检验这些取舍。

## 逐项消融

| 候选项 | 删除后的替代/损失 | 结论 |
| --- | --- | --- |
| `openEmbeddedResource` | 删除后，应用需自行解释链接符号和包格式，或退回旁置文件 | 保留标准流式 Source，不暴露指针、包索引或资源对象层 |
| `WebViewWindow` | 应用需重新对接三套窗口/WebView API、COM/回调及线程生命周期 | 保留一个直接可用的窗口能力，不再拆 Host、Controller、Engine 公共层 |
| 初始 `url` | 窗口不知道要加载什么；自动寻找服务器会重新引入隐藏组合根 | 保留必填；也从它推导允许的源，不再要求传一份 `origin` |
| `title` | 只能硬编码示例应用名称，丢失已有的应用窗口属性 | 保留普通字符串；不为它引入 AppIdentity/WindowState |
| `run()` | 调用者要自行运行各平台 UI 消息循环，或构造窗口就隐式阻塞 | 保留显式同步运行边界，负责的只是这个窗口，不是整个应用 |
| `requestClose()` | 后端失败或自动化完成时，组合根无法有序结束正在阻塞的窗口，只剩用户点击关闭/杀进程 | 保留可从其他线程调用的非阻塞请求；不暴露 native destroy |
| 窗口单独的 `close()`/`dispose()`/`awaitClosed()`/`AutoCloseable` | 窗口资源只存在于 `run()` 动态作用域，`finally` 已负责释放 | 不加第二套窗口销毁协议；流式资源则遵守标准 Source 的 close 契约 |
| `offscreen` | 产品不需要；UI 测试使用 Compose 能力，原生集成使用自有图形 runner | 删除参数及三平台分支；不把测试环境需求放进公共窗口 API |
| `persistentBrowserData` | 业务数据归 Native；当前没有恢复浏览器状态的需求 | 删除选项，固定临时上下文；必要的 Windows 目录创建/清理仍由内部负责 |
| `LocalServer`/`RpcServer`/`registerService` 包装 | Ktor 已有引擎生命周期，kRPC 已有 typed 注册与路由；包装不增加独有能力 | 不进入最小公开集；既有安全集成逻辑移至应用私有组合代码，不是删除检查 |
| `AssetStore`/`Resource`/`exists`/`list`/MIME 注册 | 标准 Source、可空返回值和 Ktor 响应足够服务现有前端 | 删除；不为流式资源增加 DTO 或 HTTP 层 |
| `navigate`/`evaluate`/JS 消息桥 | 首次加载用构造参数；业务调用用 typed kRPC；页面路由/内容内弹窗由 Compose 负责 | 不公开。测试注入不据此升级为产品 API |
| `application {}`/启动总函数/CLI/目录与服务工厂 | 普通 Kotlin 控制流、应用自己的参数和目录代码即可组装 | 删除框架入口；示例应用可以有私有辅助函数 |
| 关闭回调、状态流、原生句柄、多窗口/托盘/几何配置 | 当前单窗口的自然关闭使 `run()` 返回，程序主动关闭用 `requestClose()`；未有额外需求 | 暂不提供，不预埋通用窗口框架 |

## 必须明确的行为契约

### 嵌入资源

- `path` 是打包时的精确相对名称；找到则返回独立游标的 `kotlinx.io.Source`，未找到则 `null`。打开不物化完整 payload，读取最多按 8 KiB 分块。调用者负责 `use` 关闭；流不要求线程安全，也不附带 HTTP/MIME 元数据。
- 不保留整文件 ByteArray 读取入口，不用整文件 Buffer 伪装流；消费者主动 read-all 仍可累积全部内容。Ktor 用不带长度的 `respondSource` 接入并关闭，细节见 `resources/streaming-resources.md`。
- 包损坏必须显式失败，不能伪装成资源不存在；原始链接指针与索引保留为内部实现。最终使用这项能力的可执行文件必须链接资源对象。
- HTTP 路径的解码、规范化和拒绝规则仍由服务层执行；精确资源查找不等于 HTTP 路径安全策略。

### 窗口与生命周期

- `url` 在构造时校验。本期只接受现有应用使用的 `http://127.0.0.1:<port>/...`，拒绝凭据及无效端口；不能把 URL fragment 中的 capability 记录到日志/异常。构造不会访问网络或创建 native 对象。
- 只允许初始 URL 同源的顶层文档导航，新窗口请求直接拒绝；这不是 Compose 页面路由，也不代替服务器鉴权。不以一个 `allowNavigation` 回调把默认安全边界交出去。
- `run()` 必须在平台 UI 主线程调用；错误线程、重复运行或已有活动窗口时，在 native 调用前抛出 `IllegalStateException`。每实例最多运行一次，本期同一进程只支持一个活动窗口。它创建窗口、加载初始 URL、运行消息循环，并在自然关闭或关闭请求后退出；Windows 异步环境/控制器初始化也在此作用域内驱动消息循环，不假设全部同步完成。
- `requestClose()` 线程安全、幂等、非阻塞。运行前请求关闭会取消显示，第一次 `run()` 无窗口返回；运行中投递关闭；结束后无操作。不能在调用者线程或原生回调栈上直接销毁 COM/GTK/WebKit 对象。
- `run()` 的初始化失败、事件循环结束和异常路径都必须清理窗口资源；回调异常不能跨 C ABI。完成必要的晚到回调处理；Windows DLL 和静态 SDK vtable 按进程寿命保活，不再维护模块卸载协议。若异步创建回调超出有界清理期，保留其状态以避免悬空，并显式报错，不谎称临时 profile 已清除。
- 正常关闭返回 `Unit`；原生初始化失败、初始文档真实加载失败或主 Web 内容进程意外终止，保存内部失败、结束消息循环并清理，再由 `run()` 抛异常。不把关闭/策略主动取消导航当成引擎故障，也不为此新增 Ready、CloseReason 或通用事件流。Canvas/业务就绪仍由页面通过应用协议确认，native load 完成不能代替它。
- 清理保证限于受控返回/抛错，不承诺宿主进程崩溃或被系统强杀后执行 `finally`。`requestClose()` 的线程安全不等于 POSIX signal-handler 安全；若应用处理系统信号，需先转交正常执行上下文。
- 窗口不能启动/停止服务器、调用服务工厂、删除业务目录或退出进程。调用者在 `run()` 外部的 `try/finally` 决定其他资源的生命周期。
- 不额外要求新的 Dispatcher、ApplicationScope 或释放令牌。线程与事件循环约束是当前系统宿主的边界，不靠 DSL 掩盖。

### 固定浏览器上下文

- 每次运行窗口使用临时浏览器上下文：Mac 非持久 WKWebsiteDataStore、GTK ephemeral context、Windows 独立临时 UDF。没有公开持久性或 profileDirectory 配置。
- 业务数据和权限判断由 Native 应用负责，与执行线程是否为主线程无关。浏览器状态可丢弃，但不能因此删除 Origin/capability/导航等安全检查。
- Windows 仍可能写磁盘；关闭后按既有回调保活和有界重试策略清理自己创建的目录。清理失败显式报错，晚到回调未结束则安全保留目录；不承诺零写入或取证级擦除。实际隔离和清理仍待目标 runner 验证。
- 不读取、迁移或删除旧持久 profile，也不触碰 Native 业务数据。固定策略减少接口和分支，不声称启动、RSS 或磁盘性能已经改善。

## 调用者如何组合

以下 `createDemoServer` 是应用私有辅助函数，不是另一个框架入口；内部直接使用 Ktor/kRPC，并保留现有安全检查：

```kotlin
val backend = createDemoServer(
    service = NativeService(dataDirectory),
    openResource = ::openEmbeddedResource,
)
try {
    backend.start()
    val window = WebViewWindow(
        url = backend.url,
        title = "Kauri",
    )
    // 应用可将 window::requestClose 交给自己的失败处理/测试完成处理。
    window.run()
} finally {
    backend.stop()
}
```

- 服务实例先由应用创建，注册直接用 `rpc { registerService<DesktopService> { instance } }`。不是把同一服务再包进 Kauri 泛型协议。
- 应用可以只启动服务做无 GUI 测试、只读资源，或把另一个自行管理的合规本地地址交给窗口。
- 移到应用的现有代码包括 loopback/临时端口、每次启动 capability、Host/Origin/RPC 路径检查、静态路径与响应头策略。此设计减少公开间接层，不宣称这些必要代码会消失，也不授权调用者删掉当前安全门禁。
- 框架原生内部可以保留负责 ABI/所有权的辅助对象；“原子”不意味着把 COM 引用计数暴露给应用。

## 验收要求

- 分别验证资源读取不依赖窗口/Ktor、窗口不依赖 kRPC 服务、应用可独立管理 Ktor；框架不导入应用契约。
- 测试运行前/运行中/结束后的关闭请求、重复运行拒绝、错误线程拒绝、部分初始化失败及自然关闭后的清理；禁止只验证 smoke 特殊路径。
- 普通窗口的真实 Canvas、kRPC/Flow、自然关闭及 native 导航/新窗口拒绝必须在自有图形 runner 验证。没有 runner 时明确记缺口，不能用旧离屏产物、HTTP 403 或 Compose 场景测试冒充原生宿主回归。
- 测试完成通知可走应用拥有的 loopback 测试端点；平台策略观测和必要注入可保持内部测试接线，不应仅为减少公开 API 而丢失覆盖。
- 四目标重新编译链接，核对配置/增量、资源链接和 IDEA header-only 导入边界；Mac 运行与其他平台的编译覆盖分开记录。Windows/Linux GUI 门禁仍未补齐。

## 来源与证据边界

设计检索日期：2026-10-22。以下为官方文档摘要，不是源码摘录；消融时对照的旧 `LocalServer.kt`、`DesktopHost.kt`、`EmbeddedAssets.kt` 已被删除。当前验证与未覆盖项见 [临时浏览器上下文](ephemeral-browser-context.md)，不能用本文的设计要求代替运行证据。

- [Ktor 代码配置](https://ktor.io/docs/server-configuration-code.html)：支持 loopback 配置、`port = 0` 和 `resolvedConnectors`；[EmbeddedServer API](https://api.ktor.io/ktor-server-core/io.ktor.server.engine/-embedded-server/index.html) 提供启动/停止，无需再造同形生命周期。文档可能随最新版变化，迁移仍以仓库固定版本为准。
- [kRPC Ktor 集成](https://kotlin.github.io/kotlinx-rpc/krpc-ktor.html)：服务可直接接入现有 Ktor 路由；[RPC Servers](https://kotlin.github.io/kotlinx-rpc/rpc-servers.html)要求按契约而非实现类注册。当前资料分别标注 0.10.3/0.10.4，仓库固定 0.10.4 的实现已有直接调用证据。
- [Apple WKWebsiteDataStore](https://developer.apple.com/documentation/webkit/wkwebsitedatastore)：配置期选择默认持久存储或非持久存储；[WebKitGTK ephemeral context](https://webkitgtk.org/reference/webkit2gtk/unstable/ctor.WebContext.new_ephemeral.html)说明其上下文数据不持久化。GTK 链接覆盖 4.1/2.41.92 文档，不构成所有受支持系统的兼容性证明。
- 引擎故障由平台原有事件检测，无需公开第二套事件模型：[WebView2 进程事件](https://github.com/MicrosoftDocs/edge-developer/blob/main/microsoft-edge/webview2/concepts/process-related-events.md)、[WKNavigationDelegate 失败/终止](https://developer.apple.com/documentation/webkit/wknavigationdelegate/webview%28_%3Adidfailprovisionalnavigation%3Awitherror%3A%29?changes=l_5&language=objc)、[WebKitGTK 进程终止](https://webkitgtk.org/reference/webkit2gtk/stable/signal.WebView.web-process-terminated.html)。这些证明事件存在，不代表当前宿主已全部接线。

## 规格复核历史

- 先完成上述自行消融，再用 agy Gemini 3.8 Flash Medium 反向审查；会话 `e465e064-6f37-4291-adbb-012f9487bfda`。这是设计/文档复核，不是代码或平台运行验收。
- 采纳：补足异步初始化驱动、错误线程拒绝、故障与正常关闭的区别，以及强杀/signal-handler 的清理边界。统一通过既有 `run()` 抛错，不新增事件层。
- 不采纳：用回调才能拿到关闭函数的顶层启动器替换窗口句柄；它把直接持有和传递句柄变成获取回调，并没有消掉生命周期状态。保留两个直接操作，不新增 Scope。
- 不采纳：资源读取必须藏在 Ktor 内、必须有 native Ready 回调、把离屏改成隐藏即可。它们分别破坏能力独立性、混淆页面业务就绪、或不足以保证真实渲染。
- 用户后续确认删除原生离屏和持久性选项。浏览器固定临时化但不承诺全平台纯内存；图形测试隔离交给 runner，不扩大为窗口/Profile 框架。
