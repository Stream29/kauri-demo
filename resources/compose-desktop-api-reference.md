# Compose Desktop API 设计参考

检索：2026-10-22 01:19 UTC。范围：官方文档及 JetBrains core 的 `jb-main` 源码；这是当前设计参考，不是仓库所用 Compose 1.11.1 的精确发布源码或兼容性证明。

## 官方设计证据

- [`application` 源码](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui/src/desktopMain/kotlin/androidx/compose/ui/window/Application.desktop.kt)：入口接收 composable 的 ApplicationScope 内容；应用作用域提供 `exitApplication`，负责组合及效果的生命周期，而不是业务服务注册。
- [`Window` 源码](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui/src/desktopMain/kotlin/androidx/compose/ui/window/Window.desktop.kt)：窗口属性和关闭请求属于窗口 API；窗口加入/离开 composition 时创建/销毁。`WindowState` 用于状态控制与观察，不是要求应用手工组装原生宿主对象。
- 同一源码中的 `singleWindowApplication` 将 `application + Window` 包成快捷入口，采用退出应用的关闭处理。[官方单窗口说明](https://kotlinlang.org/docs/multiplatform/compose-desktop-top-level-windows-management.html#create-a-single-window-application)明确区分简单单窗口与需要更多控制的通用入口。
- [平台入口说明](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-entry-points.html#on-desktop)：Desktop API 属于 JVM 桌面集成；Web UI 有独立入口。不能把 Native 里的普通闭包直接当作 Wasm 侧的可组合内容。

## 对当前 Kauri API 的判断

- 当时的单入口确实删掉了外部组装链，但“公开符号 8 → 1”不是最终设计标准；它仍同时承载应用标识、窗口属性和后端服务工厂。
- 用户随后明确：只做原子能力，组合根交给调用者。学习 Compose 的职责边界，不等于让 Kauri 再提供一套应用组装 DSL。
- 因此，旧的单入口消融只保留为实现历史，不再作为目标 API。资源与窗口能力可以独立公开；服务直接使用 Ktor/kRPC，不以公开符号最少或统一包装作为评价标准。

## 已确认方向，现已实施

- 不采用此前提出的 Kauri `application { rpc(...); window(...) }` 集成式草案。
- `app/desktop` 自己创建业务服务，选择前端资源，启动本地服务，将端点传给窗口，并安排关闭顺序；框架不要求新的 ApplicationScope、Builder 或必经的配置对象。
- 能力内部仍管理安全边界及各自资源；窗口不暗中创建/关闭业务服务器，本地服务不暗中创建窗口。组合根可以用普通 Kotlin 控制流和资源作用域完成连接。
- 随后已确认原子 API 规格并完成源码重做；类型名、生命周期与运行证据分别见 `resources/atomic-framework-api.md` 和 `resources/atomic-rewrite.md`，不再保留单入口兼容层。
