# 根构建清理与 IDEA 导入修复

日期：2026-10-21。对应用户要求：删除根构建多余内容，并检查实际 IDEA Gradle sync 失败。

## 根构建

- `build.gradle.kts` 仅保留三行 KMP classpath `apply false`。没有 `base`、根 `check` 或根 `assemble`。
- release 脚本直接选择已有的四个模块 release-link 和三个 Mac 测试任务；没有将包装搬到新插件、扩展或其他自定义生命周期任务。
- KMP classpath 声明保留此前实测的必要性：避免不同模块插件组合产生 build-service 类加载冲突。

## 实际失败原因与修复

- 本机 IDEA 日志及 daemon 1167702 的日志确认：导入准备执行 `CInteropProcess` 时，因为迁移前 `build/native-deps/{linuxX64,linuxArm64,mingwX64}/{libwebview.a,SHA256SUMS}` 不存在而验证失败；这是历史 wrapper 基线问题。
- 这些是 Mac 准备的链接产物，不该成为编辑机器生成绑定的前提。此次不是通过删除全局缓存、复制 SDK 或跳过 cinterop 来规避问题。
- cinterop 改为仅用仓库的公共 C 头（仍由 KGP 自身工具链生成绑定），移除 `staticLibraries`、`libraryPath` 和 archive/SHA 输入；`.def` 保存 `-lwebview` 及系统链接项。
- 历史 wrapper 修复曾通过标准 `linkerOpts` 跟踪 archive；当前直接宿主只保留 Linux SDK link inputs 和各平台系统框架/库，IDE cinterop 只读取仓库声明头。
- Mac 上临时移走整个 `build/native-deps`，强制执行 `:framework:prepareKotlinIdeaImport --rerun-tasks`：成功。目录由 trap 恢复；没有配置 IDE 专用构建分支。

## 导入后的公共化诊断

- IDEA 实际导入后，`EmbeddedAssets.kt` 报资源长度返回值跨平台位宽不一致：C `unsigned long` 在 Windows 为 32 位、在 Linux/macOS 为 64 位。
- 接口改用 `uint32_t`。资源 archive 原本限制在 2 GiB 内，固定 32 位与格式/运行时约束一致；没有添加 `UnsafeNumber` opt-in 压制诊断。
- 普通导入准备请求已自动检测头文件变化、重建绑定并公共化；不需额外自定义生成任务或强制关闭公共化。

## 实际 IDEA 验证

- 用户明确允许本机仅做 IDEA 模型和必要 cinterop 绑定生成；应用和 webview 编译仍在 Mac。
- 经 IDEA API 后台刷新 Kauri，不激活工具窗、不导航错误、不重启 IDE，也不修改全局 JVM/缓存设置。
- 显式使用检测到的现有 daemon JVM（本次 JDK 26），最多两个 worker；日志确认复用用户的 daemon 1167702。没有停止本机用户 daemon。
- IDEA 日志中已将任务 ID 与 Kauri 路径对应：`RESOLVE_PROJECT:4/7` 的 `onSuccess` 和 `onImportFinished` 均确认，未把其他项目的导入事件混入证据。
- MCP 模块列表由空恢复为 76 个项目/源码集条目，包含四个 Gradle 模块及 Native/Wasm 源码集；这是 IDE 的源码集表示，不是新增 76 个业务模块或 JVM 后端。
- 根脚本、framework/desktop/frontend 脚本、Native 宿主公共代码、资源读取代码的 IDEA 文件诊断均无 ERROR；位宽错误已消失。不据此宣称全项目逐文件静态分析完成。

## 远程回归与证据

- 四目标 release、6 项 Native、4 项资源测试、11 项执行策略、Mac 单文件离屏 Wasm/kRPC/Canvas/Flow/策略及 7 项后端安全检查通过。
- 最终配置重复构建复用配置缓存；单文件尺寸、hash 和平台运行覆盖见 [validation.md](validation.md)。Windows/Linux GUI 覆盖没有因本轮修复而扩大。
- `build/review/root-sync-{idea-before,import-no-native-inputs,fixed-width-import,final-release,final-reuse,policy}.log` 保存失败、缺失输入测试、绑定再生成和发布回归证据；IDE 成功日志另外保存限定的 Kauri 事件。
- 三个临时工具脚本已删除，输入目录已恢复，仅停止本任务的空闲 Mac daemon 36330；本机用户 daemon 和 IDE 保持运行。prepared inputs、下载及 IDE 绑定缓存保留，未创建 commit。

## 官方依据

- [Kotlin definition file](https://kotlinlang.org/docs/native-definition-file.html)：C 头生成绑定、`linkerOpts` 与可选的 `staticLibraries` 是不同职责。
- [KMP Native binary/cinterop DSL](https://kotlinlang.org/docs/multiplatform/multiplatform-dsl-reference.html)：用标准 cinterop 与 binary linker 配置，不另造生命周期任务。
