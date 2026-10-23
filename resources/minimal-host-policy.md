# 历史记录：最小宿主策略

> 本文分析的是迁移前的第三方 wrapper 基线。当前实现已改为 Kotlin/Native 直接 AppKit/WKWebView、Win32/WebView2 COM 和 GTK/WebKitGTK；本文不证明当前运行覆盖。

# 最小宿主：profile 与离屏

日期：2026-10-21。基于仓库固定的 webview 0.12.0 源码、现有宿主和官方文档分析；本轮未运行新构建或修改宿主。

后续用户已授权实施收敛；本页保留迁移前分析，当前实现见 [minimal-static-host.md](minimal-static-host.md)，确认的边界见 `checklist/architecture.md`。

## 已确认的一期范围

- 页面路由、返回栈和内容内对话框由 Compose Wasm 负责；不建立另一套原生导航或原生弹窗系统。
- 原生多窗口/popup 支持不在本期范围。拒绝意外的新窗口请求属于边界约束，不是实现多窗口功能。
- WebView 仍需防止文档跳离应用 origin；这是既有安全约束，不负责 Compose 路由，也不能靠“不主动调用跳转”保证。

## Profile 是浏览器数据，不是应用账号系统

- 包括 cookies、缓存、localStorage、IndexedDB 和部分权限记录。业务笔记/设置仍由 Native 后端持久化；两类存储无需合并。
- “单文件分发”不意味着运行时不产生数据目录；WebView 缓存和业务数据可以位于用户目录。
- 本期无需多 profile 管理器、账号切换、浏览器数据迁移、跨平台统一缓存布局。
- 建议普通运行采用单一应用数据上下文；只在实际需要时固定独立、可写的数据位置。该默认策略是建议，尚未实施或视为用户确认。
- 当前 loopback 端口每次随机；localStorage 按 origin 隔离，因此固定 profile 并不能保证下次启动读到上次端口的 localStorage。业务持久化继续经 kRPC 进入 Native；不要为此改成固定端口。
- cookies 的隔离规则不同于 localStorage，不能把随机端口当作全部浏览器状态的隔离手段。

### 固定版库的默认值与修改时机

| 平台 | 固定版 webview 路径 | 最小定制与边界 |
| --- | --- | --- |
| macOS | 新建 WKWebViewConfiguration，未指定 websiteDataStore；系统默认持久化 | 临时会话可设 nonPersistent，但要在创建 WKWebView 前设置；拿到已建好的 native handle 不能再替换该初始化配置 |
| Windows | 显式传入 `%APPDATA%/<可执行文件名>`，不是 WebView2 SDK 的默认相邻目录 | 单应用可以先用此值；稳定应用标识/测试临时目录若有要求，需要创建 environment 前指定。SDK 文档支持环境变量覆盖，但未验证固定版内置 loader 路径的等价行为；不应依赖全局系统环境变量 |
| Linux | `webkit_web_view_new()` 使用默认 context | 临时会话可通过 ephemeral context/view 建立；ephemeral 属性为构造期设置，不能创建后切换。精确数据路径同样需要核实创建期接入点 |

- 固定版公共 C API 没有这些创建前配置入口。需要定制时，可以评估在少数构造点维护补丁、增加小型配置入口，而非重写宿主；这仍是需要验证的实现选项，不是现成的无补丁方案。
- 现有 Mac smoke 与静态库探针使用临时业务目录，但并未显式采用 nonPersistent WebKit store；不能将此前通过理解为浏览器数据已完整隔离。
- 测试建议采用临时浏览器数据上下文；Windows 若以临时磁盘目录实现，应在 WebView/浏览器进程释放后清理，不能假定销毁窗口就已释放全部文件。

## 离屏是测试模式，不是产品渲染架构

- 这里的离屏指真实系统 WebView 执行 Wasm、绘制并供截图，但不显示或激活用户窗口；不是软件重写渲染器、视频流或无图形环境运行。
- 普通产品运行只创建一个正常窗口；不需要对外发布通用 offscreen API。
- 自动化需要窗口/渲染目标尺寸、事件循环、必要的后台调度设置、完成/超时信号与资源释放。隐藏窗口不保证 Canvas/动画会持续运行。
- 已有 Mac caller-owned 屏外窗口的实际测试可复用；这是该平台可行的证据，不是其他平台无需验证的证据。
- Linux 仍通常需要图形会话；测试 runner 可以用独立虚拟显示环境，不必把这部分能力塞进产品框架。
- Windows/Linux 尚未完成静态库方案的 GUI 验证；离屏保留为测试覆盖缺口，不设计通用抽象。

## 复杂度判断

- 当前单窗口、本地可信前端、Native 保存业务数据的范围内，profile 与离屏均可收敛为少量配置/测试适配；不是继续维护完整自研宿主的充分理由。
- 必须保留创建时机、回调寿命、后台绘制和退出清理的验证，但不因此设计 profile 框架、原生路由或多窗口管理器。
- 静态库的 GTK/WebKitGTK 链接输入需求与这些运行时功能无关；删去路由/弹窗产品能力不会消除 Linux 交叉链接 SDK。

## 来源与证据

- [Apple：websiteDataStore 默认持久化及 nonPersistent 配置](https://developer.apple.com/documentation/webkit/wkwebviewconfiguration/websitedatastore)。
- [Microsoft：user data folder 内容、目录权限、共享与清理](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/user-data-folder)。
- [Microsoft：创建环境参数及环境变量覆盖](https://learn.microsoft.com/en-us/microsoft-edge/webview2/reference/win32/webview2-idl?view=webview2-1.0.2151.40)。
- [WebKitGTK：默认 context 与构造期 is-ephemeral 属性](https://www.webkitgtk.org/reference/webkit2gtk/stable/class.WebView.html)。
- [HTML：localStorage 的 origin 约束](https://html.spec.whatwg.org/multipage/webstorage.html#the-localstorage-attribute)；随机端口影响为结合本项目启动方式作出的推论。
- 固定版上游实现：`resources/webview/core/include/webview/webview.h`，GTK/Cocoa 创建路径与 Windows `embed`。
- 实际运行覆盖见 [静态库测试报告](static-webview-spike.md)；本分析未新增平台运行证据。
