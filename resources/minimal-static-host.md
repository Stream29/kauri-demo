# 历史记录：统一静态宿主方案

> 本文记录迁移前的 `webview/webview` 静态包装库基线和决策过程。2026-10-21 已由直接 Kotlin/Native 系统宿主替代；请以 [validation.md](validation.md)、[checklist/architecture.md](../checklist/architecture.md) 和可执行 Task Tree 为准。本文中的 archive、C API 和 `resources/webview` 路径不是当前构建依赖。

# 最小静态宿主与构建收敛

日期：2026-10-21。按用户确认的目标先更新架构与 Task Tree，再实施迁移；不升级 Kotlin、Compose、kRPC、Ktor 或 Gradle。

## 当前实现

- 四目标共用 webview 0.12.0 静态 C API。正常窗口、WebView 创建/销毁和事件循环由上游负责；旧的 Win32/COM、GTK/dlopen 自写宿主及独立 loader 适配已删除。
- Kotlin/Native 保留本地 Ktor、嵌入资源和 typed kRPC。Compose Wasm 负责应用导航与内容内对话框；不建立原生路由或多窗口系统。
- 平台代码只保留文档同 origin 限制、新窗口拒绝、必要的数据/测试设置。Mac 使用 Kotlin delegate；Windows/Linux 的小型策略适配随静态库编译。
- 普通运行采用单一浏览器数据上下文；Mac/Linux 使用默认持久化上下文，Windows 位于应用数据目录 `%LOCALAPPDATA%/Kauri/WebView`，不是 exe 旁置目录。业务笔记仍由 Native 保存，不依赖随机端口下的 localStorage。
- smoke 使用非持久化 WebKit store/ephemeral GTK view，Windows 使用临时目录。Mac 已实测 nonpersistent 属性；Windows/Linux 的 GUI、绘制和退出清理仍需目标 runner，不能只凭链接推断通过。
- 不修改 webview 参考子模块；准备脚本校验固定头文件 hash，将三个创建期配置点的补丁应用于忽略目录内的副本。
- Windows 使用官方 SDK 中的 IID 常量，避免 MinGW 的 `__mingw_uuidof` 缺失特化；没有手抄 COM ABI 或新建 loader。

## 构建收敛

- 删除整个 `build-logic` 约定层及自定义任务类。模块直接声明标准 KMP targets、binaries、cinterop、依赖和 linkerOpts。
- 根构建只保留一个 KMP `apply false` 共享插件类；根生命周期包装已删除，发布脚本直接选模块任务。删除 classpath 声明后实测出现不同模块插件组合间的 SwiftPM build-service 类加载冲突，故保留它。
- 标准 KGP 任务负责 Wasm 分发、Kotlin 编译、cinterop 和最终链接。
- 特殊接线只有一个标准 `Exec` 打包前端、每目标一个标准 `Exec` 生成资源 object。脚本原子替换输出，声明资源、脚本、编译器和目标输入；链接显式依赖 producer 并跟踪 object。
- 后续 IDEA 修复将绑定与链接分离：cinterop 只用仓库公共头，`.def` 声明 `-lwebview` 和系统依赖；prepared archive 及 Linux SDK 是最终 binary 链接输入，不再打入绑定 klib 或成为 IDE 导入前提。
- cinterop 命令行附加 `-linker-option -L…` 本次未进入 klib manifest；因此 SDK 搜索路径直接使用最终 Native binary 的标准 `linkerOpts`，不依赖隐式传递。
- release 脚本负责复制、Mac strip/ad-hoc 签名、校验和与验证收据。临时文件签名后原子替换，固定签名标识，不覆盖运行中的旧 inode；普通开发链接不触发这些步骤。

## 显式依赖准备

- `prepare-native-mac.sh` 使用 KGP 的 Native 分发下载任务（需要时）及 `konanc -Xcheck-dependencies`，不自行实现 Kotlin 工具链下载器。
- SDK 下载核对 SHA-256；Linux 采用已实测的 Debian bookworm 包锁定清单。准备结果在 `build/native-deps`，下载缓存位于用户缓存目录。
- Windows 使用 MinGW 兼容的上游内置 loader；Linux 将 GNU libstdc++ object members 合入 archive，但 GTK/WebKitGTK 仍是系统动态依赖。
- 应用 Gradle 构建不调用原生准备脚本。首次准备、原生源码/SDK变更或删除准备输出后，需显式重跑；不是把额外下载藏进最终链接任务。
- 本轮使用既有暖工具链/依赖缓存；未以全新机器验证完整冷 bootstrap，也未测量跨机器构建可重现性。

## 验证与审查

- 四目标 release 已链接，Mac 单文件离屏应用、6 项 Native 测试、4 项资源打包测试、7 项后端安全检查通过。真实 Canvas 为 110 色采样，笔记/Flow/typed kRPC 和文档/新窗口限制正常。
- 重复发布与配置缓存复用、11 项脚本策略检查及框架独立任务图检查纳入本轮验证；最终尺寸、hash 和覆盖以 [validation.md](validation.md) 为准。
- agy Flash Medium 两次限时请求均无正文/零 token，不算完成审查。Flash Low 返回了三项构建意见，逐项用任务图、严格配置缓存、清理恢复及静态库变更验证核实，不机械恢复被删除的封装。
- Flash Low 在事实复核中撤回初审三项。当时的 archive 打入 klib 方案经过实际输入变更验证；后续为消除 IDE 导入的 archive 耦合，改为显式最终链接输入，不把先前探针当作新接线验证。
- 后续意见中的“FileTree 在配置期冻结”与 Gradle 的 live collection 行为不符；SDK 整目录输入仍收短为 `inputs.dir`。两个 binary 配置块分别加 SDK 搜索路径与资源 object，没有重复注入同一 flag；四平台链接也未出现其推测的 object 路径解析错误。
- 旧约定层缓存/宿主输出已清理，旧测试 XML 归档；输入变更探针已恢复/移除，仅停止本任务创建且确认空闲的 Gradle daemon。保留供下一次构建使用的 native inputs 与下载缓存，未创建 commit。
- 初次迁移没有实际 IDEA 验收；后续已完成真实导入修复及资源长度位宽修复，见 [root-idea-sync.md](root-idea-sync.md)，不再以 CLI 成功替代 IDE 结果。
- 尚未完成 Windows/Linux 的 GUI 运行验收；不宣称单文件意味着零系统依赖、性能更好或比其他宿主更可靠。

## 官方资料

- [KMP targets/binaries/cinterop DSL](https://kotlinlang.org/docs/multiplatform/multiplatform-dsl-reference.html)。
- [Definition file 与 staticLibraries](https://kotlinlang.org/docs/native-definition-file.html)。
- [Gradle 文件集合的 live 行为](https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_collections)。
- [固定版 webview 源码](https://github.com/webview/webview/tree/0.12.0)；此前隔离验证见 [static-webview-spike.md](static-webview-spike.md)。
- [GNU libstdc++ 上游许可说明](https://gcc.gnu.org/onlinedocs/libstdc++/manual/license.html)。本轮只记录链接组成与来源，不作发行合规认证。
