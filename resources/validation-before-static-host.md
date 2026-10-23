# 自写宿主时期验证记录（历史）

日期：2026-10-21。以下是统一静态宿主迁移前、仍使用约定插件/自写平台适配时的结果。旧 hash 不认证当前产物；当前记录见 [validation.md](validation.md)。

## 构建环境

- 构建主机：`stream@macbook`，Apple Silicon，macOS 26.5.1，Xcode。
- Kotlin 2.4.0、Compose Multiplatform 1.11.1、kRPC 0.10.4、Ktor 3.3.0、Gradle 9.6.1。
- Gradle helper 显式指定已检测到的 daemon JDK / JVM 参数，验证复用，最多两个 worker；JDK 为 GraalVM 21.0.8。
- Node 24.12.0 由 nvm 管理。所有 Gradle、原生链接和 C/C++ 交叉编译均在 Mac 上完成。
- 本轮删除应用 binaries、嵌入资源和 host objects 后，四个 release link 与三个宿主 producer 实际执行，构建为 1m 51s；后续同一请求为 781ms，108 个任务中 6 个执行、102 个 up-to-date，配置缓存复用。删除 archive / SDK 头文件后的离线恢复均命中构建缓存。使用暖依赖缓存，不是冷构建或开发体验对比测量；bootstrap / Mac-only 等检查属于上一轮治理记录。

## 单文件产物

| 目标 | 文件 | 字节 | MiB | 实际验证范围 |
| --- | --- | ---: | ---: | --- |
| macosArm64 | `build/release/macosArm64/Kauri` | 16,993,824 | 16.21 | Mach-O arm64；原生测试、离屏 WKWebView、真实 canvas 像素及 kRPC 端到端通过 |
| mingwX64 | `build/release/mingwX64/Kauri.exe` | 19,524,096 | 18.62 | PE32+ x64 GUI；系统 DLL 导入表审计通过；未在 Windows 运行 |
| linuxX64 | `build/release/linuxX64/Kauri` | 18,704,512 | 17.84 | ELF x86-64；动态依赖审计通过；本机低优先级 `--licenses` 启动成功，未启动 GUI |
| linuxArm64 | `build/release/linuxArm64/Kauri` | 18,172,888 | 17.33 | ELF aarch64；动态依赖审计通过；本次未使用目标架构 runner，未运行 |

- Mac 产物经过 `strip -x` 和 ad-hoc 签名；Windows / Linux 保留 Native release 链接器默认符号处理。
- 数字为单个可执行文件的长度，不包含系统 WebView、系统动态库、缓存或笔记，也不是运行内存。
- 六个嵌入 web 资源的 archive 为 **12,971,920 字节（12.37 MiB）**。
- 其中两个 Wasm 文件分别为 8,652,729 和 3,774,033 字节，JavaScript 为 539,079 字节；其余是 HTML 和许可证说明。

## 模块边界复核

- Gradle 模块为 `:framework`、`:app:shared`、`:app:desktop`、`:app:frontend`；框架源码和依赖不引用应用契约或服务实现。
- 框架独立编译与 3 项 Native 测试通过。其测试任务图没有应用编译或链接任务；Kotlin 插件仍会调度全局 SwiftPM 依赖锁元数据任务，这不代表框架依赖应用代码。
- 应用使用 `DesktopApplication` 提供名称、标题和服务工厂；`LocalServer` 通过路由注册回调接入应用的 typed kRPC 服务。
- npm workspace 名称已随模块路径更新，并清理旧 workspace 生成目录及锁文件条目。最后一次正常发布构建没有绕过锁文件校验。
- 前端与宿主 objects 通过声明式产物传递；链接任务跟踪实际文件和生产者，避免仅靠路径或任务名推断依赖。
- 构建逻辑位于独立 `build-logic`：四个预编译约定脚本与必要的类型化任务；删除手写插件类、根 `apply false` 列表和单用途扩展，保留产物变体、缓存和 manifest 约束。治理、简化与独立 agy 审查见 [build-review.md](build-review.md)。

## 已通过的检查

- **6 个 Native 测试**：应用契约 2 项、应用持久化 1 项、框架路径/配置边界 3 项；0 failures / 0 errors。
- **14 项构建逻辑单测及插件校验**、**11 项脚本策略检查**通过；缓存恢复、离线 SDK、Mac-only 发布和新 daemon 初始化/复用经过实测。
- **独立文件启动**：Mac smoke 只复制一个可执行文件到临时空目录，不提供旁置 web 资源。
- **真实 Compose 绘制**：从 Compose 的 ShadowRoot 中捕获实际 canvas，64×64 采样有 **110 种颜色**，排除空白画布。
- **真实 typed kRPC**：Wasm 调用 Native 系统信息，保存并读回 Unicode 笔记，拒绝超长输入且保留原文件，接收 Flow 的 1、2、3 号事件。
- **7 项 HTTP / 边界检查**：健康检查 200；foreign Origin、foreign Host、缺少 WebSocket Origin、错误 capability 均为 403；原始及编码路径穿越均为 404。
- **产物校验**：四个从远程同步的文件均通过 `sha256sum -c SHA256SUMS`。
- **Windows 单文件依赖**：PE 导入仅包含 Windows 系统 DLL；不导入 `WebView2Loader.dll`、libstdc++ 或 libgcc 的伴随 DLL。
- **Linux 非 GUI 启动**：最终 linuxX64 二进制在本机以低优先级运行 `--licenses` 成功，没有运行本机 Gradle 或打开窗口。
- **签名**：Mac 产物通过 `codesign --verify --strict`，仅为 ad-hoc 签名，不是公证。
- **清理**：Mac 测试进程已退出，临时 standalone 和 smoke 数据目录已移除；仅停止了本任务创建且空闲的 Gradle daemon，其他 daemon 未动。

离屏检查不激活窗口。测试专用的定时 frame clock 避免 WebView 对遮挡窗口的 animation frame 节流；正常窗口保留原生 frame clock。因此此测试验证真实渲染及通信，但不是正常窗口的帧率或延迟基准。

实际 smoke 输出：

```text
PASS: Compose painted (110 sampled colors); Native systemInfo;
Unicode note round-trip; rejected oversize; Flow 1,2,3; unauthorized RPC 403
```

## 校验值

```text
7c945e79acd34ea041392fc81c4827a60ef6567f96727e5518a6459a27c2f9b3  macosArm64/Kauri
e015c46681c41507fcd6d39a9fe495105415df6f52176384fc64fda409c7a1d4  mingwX64/Kauri.exe
290dd4285194f9dc42accf33371cf92e9e3a91ab4f0734faba24bb4cd68b9e73  linuxX64/Kauri
60f5c4a2a58ae8cdc9c1395c70fb3aa42314d61f525e5a0aad7195b24ace90b5  linuxArm64/Kauri
```

## 证据位置

- 本机与 Mac：`build/release/` 包含四个单文件产物及 `SHA256SUMS`。
- 本机与 Mac：`build/smoke/window.png` 为实际 Compose canvas 捕获；`result.txt` 和 `process.log` 为运行结果。
- 本机与 Mac：`build/review/simplify-{check,release,reuse,restore,policy}.log` 记录本轮构建、运行和缓存验证；先前治理日志索引见 `build-review.md`。helper 当前客户端日志和临时所有权位于 `.gradle/kauri/`，不进入 Git。
- 当时的 Native XML 位于三个模块的 `build/test-results/`；旧构建逻辑 XML 已归档到 `build/review/legacy-build-logic-tests.xml`，不用已删除的模块路径作为当前测试入口。
- 构建产物、日志、能力 URL 和临时笔记均不进入 Git。

## 尚未验证或实现

- Windows、Linux arm64 的原生运行，以及两个 Linux 目标的 GUI 端到端测试。
- Windows 系统需要 WebView2 Runtime；Linux 需要 GTK 3 / WebKitGTK 4.1 和相应系统库，并且 WebView 必须支持 WasmGC。
- 自动更新、安装器、Developer ID / Authenticode 签名、公证、热更新和生产级权限策略。
- 没有同功能的 Electron、Tauri、Compose Desktop 对照；不能据此宣称体积必然更小、开发体验或性能必然更好。
