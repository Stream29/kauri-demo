# 构建系统治理与独立审查

初次治理：2026-10-20；简化回合：2026-10-21。范围是构建系统，不包含平台宿主重写或应用依赖升级。

本页为此前治理历史。后续已按用户授权迁移统一静态宿主并删除约定插件层，当前设计/审查处置见 [minimal-static-host.md](minimal-static-host.md)，当前交付证据见 [validation.md](validation.md)。

## 2026-10-21：Task 必要性专项审查

- **Gemini 3.8 Flash Medium 已完成独立源码审查及一次事实复核**。初审提供完整任务、注册、测试、archive 读取器和发布脚本，没有附带主助手先前的分类结论；复核才提供反证和遗漏的初始化背景。
- Opus、Argon、Sonnet 先前因额度返回 429；用户提醒后改用 Flash 成功。这些失败调用不算审查结果。
- 原始初审及复核分别在忽略目录 `build/review/agy-task-necessity-flash.json`、`agy-task-necessity-flash-followup.json`。本轮只审查，没有改构建或重新运行测试。
- 当前数量核实为 **6 种自定义类型、18 个自定义实例、5 个额外聚合任务**。四平台实例展开本身不是过度设计的证据。

### 分类结论

| 类型 | 核实后的审查结论 |
| --- | --- |
| `PackAssets` | 当前运行时依赖私有 archive；保留专门实现合理。标准 Zip 不是不改运行时即可采用的替代，不意味着 Zip 必须依赖某个 C 库。 |
| `EmbedAssets` | 嵌入步骤是当前单文件方案的一部分。自定义类与标准 Exec 都能组织它；拆成生成汇编和编译两步未必减少总代码，报告最终倾向保留。 |
| `CompileNativeHost` | 保留封装有价值，特别是 Windows 的两次编译器调用。可以考察更窄的工具链初始化依赖，不直接删除前置依赖。 |
| `StageExecutable` | 非 Mac 的复制/赋权与 Mac 的 strip/签名可以分流。属于可选简化，替代时仍需避免输出目录重叠、漏签名或失败后误认产物有效。 |
| `ReleaseChecksums` | 优先审视全局加四个局部清单的重复交付策略；可收敛任务或转移到发布流程，但生成清单和验证清单不是同一操作，Mac-only 消费者必须一起调整。 |
| `ExtractWebView2Headers` | 标准 archive/Sync 可以接管解包；是否取消自定义类取决于校验放在哪里。必须明确校验、必需头文件、旧输出处理及缓存行为的变化。 |

### 优先简化点与纠错

- **优先级 1：清单策略**。[五个 checksum 实例](file:///home/stream/ACodeSpace/demo/Kauri/build-logic/src/main/kotlin/kauri.desktop.gradle.kts#L98-L107) 可研究收敛，但单次清单必须明确目标范围，不能把磁盘残留文件当成本次产出。当前 Mac-only 清除全局 manifest 是防陈旧，不是修复 Gradle 检查报错。
- **优先级 2：SDK 解包**。[手写 ZIP 遍历](file:///home/stream/ACodeSpace/demo/Kauri/build-logic/src/main/kotlin/dev/kauri/gradle/SdkTasks.kt#L31-L72) 可以委托标准设施；仓库尚无 `verification-metadata.xml`，不能声称 Gradle 已自动代替 SDK SHA-256 校验。采用 Sync 也不自动保留当前的 `FROM-CACHE` 能力。
- **优先级 3：发布分流**。[StageExecutable](file:///home/stream/ACodeSpace/demo/Kauri/build-logic/src/main/kotlin/dev/kauri/gradle/ReleaseTasks.kt#L23-L67) 可缩小平台职责；不能仅因为它禁用构建缓存就判定类多余，构建缓存与 up-to-date 检查不同。
- Flash 已撤回“`providers.exec` 在顶层立即执行”的缺陷定性；源码仅构造惰性 Provider，没有配置期 `.get()`。此前 `:help` 未触发 xcrun 的实测与此一致。
- Flash 已撤回“坚决删除 main compilation 依赖”：现有依赖还承担 LLVM/sysroot 初始化。要解绑需先确定更窄的就绪点，并在隔离的冷工具目录验证，**不能删除用户共享的 `.konan/dependencies`**。
- 初审数量矛盾已修正：若只替换 SDK 与 checksum 两类，仍保留 Stage，则候选为 **4 类、12 个自定义实例**，不是 3 类，也不是总任务仅 12 个；标准替代任务仍存在。这是未实施方案，不是当前仓库状态。
- 初审的“内存指针直读”不能解释为零拷贝；当前运行时 `readBytes` 会产生 `ByteArray`。本轮没有测量性能，也未证明更多并行或更少 Task 会更快。

### 核实依据与边界

- 2026-10-21 查阅 Gradle 官方 [ProviderFactory.exec](https://docs.gradle.org/current/dsl/org.gradle.api.provider.ProviderFactory.html)：进程输出惰性获取；配置期读取时才成为配置模型输入。
- 官方 [Sync](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/Sync.html) 默认不启用构建缓存，并会清理目的目录；[依赖验证](https://docs.gradle.org/current/userguide/dependency_verification.html) 需显式元数据与信任策略，不因使用标准解包自动启用。
- [增量构建文档](https://docs.gradle.org/current/userguide/incremental_build.html) 支持通过任务运行时 API 声明输入输出，同时建议非平凡实现优先使用类型化任务。没有“现代 Gradle 不应有自定义类”的普遍规则。
- 当前网页为较新的 9.x 文档；仓库仍固定 9.6.1。以上用于语义核对，替代方案的具体 DSL、冷构建、缓存和单目标/全量发布仍需另行验证。

## 2026-10-21：简化回合

- 用户指出上一轮过度复杂，并追问根项目的 `apply false`。这不是 Gradle 多模块构建的必需模板，已删除根项目五项插件别名及中转 `checkNative` 任务。
- 五个手写插件类改为四个预编译约定脚本；删除手写 plugin ID / implementation class 注册。仅前端使用的 Wasm 设置和产物发布回到 `app/frontend`。
- 删除单用途 `DesktopPackaging` 扩展，发布统一使用仓库 `build/release`。插件版本仍由版本目录提供；约定使用的 KMP 由 `build-logic` 的 KGP 依赖提供。
- 配置角色改用原生 `dependencyScope` / `resolvable` / `consumable`，删除手动角色开关。自定义任务的输出 provider 传播生产者，不再重复 `builtBy`；KGP 链接输出的已知显式依赖仍保留。
- 保留资源 archive、嵌入对象、C/C++ 编译、SDK 校验及签名任务。这些包含实际转换或校验，不是为了复用配置而制造的插件类。

### 独立 agy 建议的取舍

- Claude Sonnet 5.5 Medium 完成只读简化审查；原始响应在忽略目录 `build/review/agy-simplify.json`。
- 采纳预编译脚本、前端配置回归模块、删除单用途扩展和原生配置角色建议。
- 未采纳“没有测试，删除 JUnit”：其输入未包含测试源码，实际已有 14 项测试。没有因审查输入不完整删除有效测试。
- 未删除四目标共享约定或 web tooling：前者有三个消费者，后者需覆盖 root 和存在 Wasm 的模块。
- 未采纳只固定 JDK 就能保证复用既有 daemon：兼容性还涉及实际 JVM 参数；用户明确要求检测和复用，不能用可能新建 daemon 的简化替代。
- 未用 `Copy` / `doLast` 替代所有任务：文件跟踪不保证签名成功后再发布，也不会自动保留 SDK 完整性校验。全局依赖验证需要独立的信任策略，不以放宽校验换取少量代码。
- 复审补充了前端发布仍需显式 `builtBy` 的文档例外，已澄清。其余“缺陷”未确认：`xcrun` 惰性求值不影响已成功的 Linux IDE 导入；跨项目同名配置合法；KGP 依赖实际引用同一版本目录；Native test binary 不是 `Executable`，测试图也未包含前端打包。

### 本轮验证

- 首轮远程 `:help :check` 通过，根项目不再包含 `apply false`；预编译脚本、插件校验、14 项构建逻辑测试及 6 项 Native 测试通过。
- 删除应用 binaries、嵌入资源和 host objects 后，四个 release link 与三个 C/C++ producer 实际执行；Mac 单文件离屏绘制、typed kRPC / Flow 与 7 项安全检查通过。
- 相同构建请求再次执行，配置缓存复用；108 个任务中 6 个执行、102 个 up-to-date。11 项脚本策略检查通过。
- 删除资源 archive 和 WebView2 头文件后，以离线构建恢复，两项任务均为 `FROM-CACHE`，四平台校验值保持一致。
- 用户追加 IDEA sync 排查：本机日志记录 10:04 的中间态脚本编译错误（`java.io.File` 被 DSL 名称遮蔽、文件级 opt-in 位置错误）；修正后，10:07:24 实际导入完成。只读取用户已有日志，没有触发本机 Gradle 或操作 IDE 窗口。

## 独立审查方法

- agy 使用 Claude Sonnet 5.5 Medium，完成独立初审与源码复审；没有工具调用、文件修改或测试执行。
- 原始响应保留在忽略目录 `build/review/`，不提交会话标识或原始执行日志。
- 以下处置区分可复现缺陷与审查中的推测；不能把静态审查当作运行验证。

## 初审发现与处置

| 编号 | 审查主题 | 处置与核实 |
| --- | --- | --- |
| A01 | 配置阶段路径求值、字符串任务名、测试拖入前端打包 | 采用类型化插件、声明式产物和明确的生产者。单纯 `.get()` 取路径不等于解析依赖，真正问题是路径/任务耦合及生产者信息丢失。只给应用可执行文件链接 UI 资源。 |
| A02 | 原生编译输入不完整 | 声明 LLVM 安装、sysroot、源码、SDK/上游头文件及目标参数；链接跟踪实际 object 文件。 |
| A03 | LLVM 通配符选择不确定 | 固定 Kotlin 2.4.0 对应的 exact distribution；已对照安装包 `konan.properties` 核实 `llvm-21-aarch64-macos-essentials-97` 与三个 sysroot。 |
| A04 | SDK 标记缓存、下载/解包生命周期 | 下载交给 Gradle 的受限 NuGet/Ivy 解析器；解包校验 SHA-256、限制头文件路径并清理过时输出。初审提到的并发下载不是当前串行单 Windows producer 的已证实故障。 |
| A05 | daemon 选择、JVM 解析及事后验证 | 按 [构建所有权规范](../checklist/build.md#build-ownership) 筛选并复用；身份值参与配置缓存校验，解析失败时拒绝猜测。 |
| A06 | bootstrap 可能启动不兼容 daemon | 无 daemon 时先启动显式 JVM 的最小临时构建；已实测重新初始化后仍通过身份门禁及缓存复用。 |
| A07 | 发布残留与 manifest 容易混同运行认证 | 签名/校验进入类型化任务，候选文件验证后替换；SHA 只声明完整性，单独的 `VALIDATION.txt` 在脚本检查通过后生成并说明 runner 缺口。没有证据支持随意改变 strip 或内存参数。 |
| A08 | 两个 release 脚本输出分叉 | Mac 入口转发到统一发布流程，均使用 `build/release/macosArm64/Kauri`；部分发布会使完整矩阵 manifest 失效。 |
| A09 | smoke 非零退出不打印日志、TERM 后可能等待不止 | 先收集状态再输出日志；只清理自己的子进程，TERM 等待后升级 KILL；支持隔离失败测试的结果目录。 |
| A10 | 版本分散与 API 疑问 | 统一版本目录和工具链描述，保留现有依赖。Node/Yarn、Wasm API、`-ldl` 不能仅凭审查中的不确定表述判为缺陷，以实际构建和官方实现核实。 |

## 额外发现

- KGP 的链接输出是原始 `Provider<File>`，适配后未保留生产者时，staging 会错误复用磁盘上的旧二进制。已显式关联类型化 link provider；验证必须清理输出并确认实际 link 任务执行，不能只看 `BUILD SUCCESSFUL`。
- 原生 host 以明确的 object 文件发布，避免配置缓存保存任务图时扫描尚未产生的输出目录。
- `FAIL_ON_PROJECT_REPOS` 与 KGP 自动添加工具仓库冲突。采用官方 `EnvSpec.downloadBaseUrl` 无值模式，保留 Binaryen 下载功能并由 settings 的受限 Ivy 仓库解析；没有降级仓库策略或禁用 Wasm 优化。
- `:check` 与不带冒号的 `check` 选择范围不同。发布流程使用限定任务路径，helper 拒绝常见的未限定聚合选择器。
- 锁和 daemon 所有权文件不能放在 `clean` 会删除的 `build/`。它们现位于 `.gradle/kauri/`，并覆盖发布和离屏运行阶段。
- 已移除 Compose 插件弃用的依赖简写。显式 Material3 仍为插件原先选择的 1.9.0，不误改成 Compose UI 的 1.11.1。

## 复审意见与处置

- 复审未发现必然阻断构建的缺陷，但提出了额外风险。模型的严重度标签不是证据，以下按具体机制判断。

| 编号 | 处置 |
| --- | --- |
| R01 参数与身份门禁 | 补充拒绝客户端诊断、外部 init 和项目/设置路由参数。原代码已启用 `pipefail` 并保留非零状态，早退客户端命令也因缺少身份观测而失败，未确认其“门禁绕过”定性。保留 `--no-configuration-cache` 诊断，实测仍经过身份校验。 |
| R02 锁采用 | 保留“实际父 PID + 环境中的 owner + 所有权文件”检查，额外检查父进程存活，增加陈旧锁的人工恢复提示。锁是协作协议，不是同用户恶意代码的安全沙箱。陈旧和无关活跃 PID 均已测试拒绝。 |
| R03 失败与中断 | 修复写 owner 失败时遗留部分文件的问题，只清理刚创建的记录。没有为审查去打断用户构建，也不自动停止未知 daemon；实际测试限定在自己创建的子进程。 |
| R04 strip 查找 | 不采纳改回未解析 dispatcher 的建议。`providers.exec` 链是惰性的；`:help --no-configuration-cache` 实测没有执行 `xcrun --find strip`。工具选择变化导致缓存失效是预期行为。 |
| R05 矩阵缺项 | 新增明确的 manifest 目录集合约束及缺项单测；完整发布必须恰好包含四目标。全量 assembly 同时更新各目标和全局校验文件，避免 Mac-only 切回全量后留下过期 sidecar。 |
| R06 文档与证据 | 以最终测试报告替换待复核计数；Linux 显示隔离变量已在脚本中实现并做拒绝测试。规范与审查记录各司其职，避免重复展开所有权流程。 |

## 最终验证

| 检查 | 实际结果 |
| --- | --- |
| 构建逻辑 | **14 项 JVM 单测通过**，`validatePlugins` 通过；构建逻辑编译警告视为错误。没有通过 TestKit 启动竞争 Gradle daemon。 |
| 应用与框架 | **6 项 Native 测试通过**：契约 2、持久化 1、框架 3；单测任务图不包含生产前端打包。 |
| 脚本策略 | **11 项通过**：worker/JVM 覆盖、未限定任务、客户端诊断、项目路由、外部 init、并发调用、陈旧锁、无关 owner、smoke 失败日志、子进程清理。 |
| 真实产物重建 | 清理应用/root 输出，并另行删除 host objects 和应用 binaries 后重建，确认四个 release link 与三个 C/C++ producer 实际执行；不接受 staging-only 的缓存旧产物结果。 |
| 运行 | Mac 单文件离屏 Canvas、typed kRPC/Flow、Unicode 笔记及 **7 项 HTTP/安全检查通过**。Windows/Linux GUI 与 Linux arm64 执行仍有 runner 缺口。 |
| 配置缓存 | 严格模式存储和复用通过；ValueSource 在命中时仍观测实际 PID。 |
| archive 缓存 | 删除 `frontend.pack` 后 **FROM-CACHE**，恢复前后 SHA-256 一致。 |
| SDK 与离线 | 暖缓存下离线解析通过；删除头文件目录后 **FROM-CACHE** 且 hash 一致。人为修改头文件时，Gradle 保守禁用该次输出缓存并重执行，正确修复；未把重执行冒充缓存命中。 |
| daemon 生命周期 | 仅停止本任务创建且空闲的 daemon，再由最小临时构建初始化新 PID；相同 pack 任务重新配置成功，下一轮缓存复用成功。 |
| 发布范围 | Mac-only 的目标校验通过，完整矩阵 manifest 被移除；随后完整发布恢复四目标及各自的有效校验文件。 |
| 无 GUI 的策略检查 | Linux 未设置显示隔离许可时立即拒绝，没有在 Linux 工作站运行 Gradle 或打开应用窗口。 |

- 完整矩阵复核记录有一次 `57s` 的构建、一次配置缓存命中的 `756ms`，以及更换 daemon 后的 `1s` 复核；它们使用暖依赖缓存，执行任务数也不同，**不构成构建速度对照实验**。
- 最新尺寸、SHA-256 和运行覆盖范围见 [validation.md](validation.md)。Mac 的真实 canvas 捕获仍有 110 种采样颜色。
- 远程证据：`build-governance-{rebuild,final,reuse,policy,pack-restore,sdk-restore,sdk-repair,unit-graph,help,bootstrap-reuse,new-daemon-reuse,mac-only}.log`；单测 XML 位于对应模块的 `build/test-results/`。

## 已核实的上游限制

- kRPC 0.10.4 插件调用 Gradle 已弃用的 `Project.getProperties`。本地 deprecation trace 定位到 `kotlinx.rpc.RpcPluginConstKt.isInternalDevelopment`，不是本仓库新增构建逻辑调用。
- 保持 Gradle 9.6.1，不隐藏该警告，也不未经兼容验证升级 Kotlin/RPC。Gradle 10 升级需要解决上游调用。
- 未进行跨机器、冷依赖缓存的可复现构建或构建速度对照实验。
