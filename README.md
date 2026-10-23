# Kauri

**Native roots. Wasm canopy.**

一个 Tauri-like 桌面实验：Kotlin/Native 后端、Compose Wasm 界面、共享 Kotlin 接口的 kRPC 通信。

## 交付目标

- **每个目标一个独立可执行文件**，直接运行即打开应用窗口。
- 目标矩阵：`macosArm64`、`mingwX64`、`linuxX64`、`linuxArm64`。
- Compose Wasm、Skia 和其他前端资源链接进 Mach-O / PE / ELF，从内存提供资源，无需旁置目录。
- 使用系统 WebView；不打包 JVM、Node.js 或 Chromium。平台构建与运行验证状态见下方证据链接。

```text
Kauri executable
├── Kotlin/Native
│   ├── direct Kotlin/Native host → platform window + system WebView
│   ├── embedded frontend assets → loopback Ktor CIO
│   ├── typed kRPC service → native APIs
│   └── app-owned UTF-8 note
└── WKWebView / WebView2 / WebKitGTK
    └── Compose Wasm → generated kRPC client → shared @Rpc interface
```

## 演示功能

- **系统信息**：通过 Foundation、Win32 或 POSIX API 获取 CPU、内存、操作系统和进程信息。
- **持久化笔记**：Compose 输入框 → typed `saveNote()` → 原生文件 API 原子写入。
- **原生推送**：Native `Flow<Pulse>` 每秒通过 kRPC 更新界面。
- **安全边界**：随机 loopback 端口、每次启动的新 capability、Host / Origin 校验、限定应用内导航。

| 平台 | 笔记位置 |
| --- | --- |
| macOS | `~/Library/Application Support/Kauri/note.txt` |
| Windows | `%LOCALAPPDATA%\Kauri\note.txt` |
| Linux | `$XDG_DATA_HOME/Kauri/note.txt`，默认 `~/.local/share/Kauri/note.txt` |

原型不暴露任意路径读写或执行 shell 的接口。

## 应用与框架

```text
app/
├── shared/       # 演示应用的 @Rpc 契约、DTO、校验
├── frontend/     # Compose Wasm UI
└── desktop/      # 可执行入口、RPC 实现、系统信息和笔记原生 API
framework/       # 独立的 Native 窗口/WebView 与嵌入资源读取
```

- 依赖方向：`app/frontend → app/shared`；`app/desktop → app/shared + framework`。`framework` 不依赖应用。
- 调用者拥有组合根：应用直接创建 Ktor CIO、注册 typed kRPC 服务、提供嵌入资源，再把 URL 交给窗口。框架不依赖 Ktor、kRPC、应用服务工厂或业务目录。
- 框架只有两类公开能力；没有应用 DSL、服务器包装、Host/Controller 层或 JS 业务桥：
- 使用直接 Kotlin/Native 平台宿主：macOS 调用 AppKit/WKWebView，Windows 调用 Win32/WebView2 COM，Linux 调用 GTK/WebKitGTK；页面导航与内容内对话框由 Compose Wasm 负责。
- 普通 KMP 脚本声明四目标、cinterop 和链接配置；没有 `build-logic`、自定义任务类或发布插件。

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

- 构造窗口不获取原生资源；主线程 `run()` 创建、运行并清理窗口，返回是完成边界。`requestClose()` 可以从其他线程调用，只请求关闭，不退出进程或停止服务器。
- 资源返回独立的流式 Source，由调用者 `use` 关闭；按需最多 8 KiB/次填充缓冲，不先复制完整资源。Ktor 接线为 `source.use { call.respondSource(it, contentType) }`，不传 `contentLength`；固定版 Ktor 的显式长度分支会先累积指定长度。见 [流式实现与验证](resources/streaming-resources.md)。
- 应用用自己的 `try/finally` 关闭 Ktor；capability、Host/Origin、RPC/静态路径安全检查保留在 `app/desktop`。浏览器导航/弹窗防护及 COM/GTK/WebKit 所有权留在窗口内部。
- 窗口固定使用临时浏览器上下文，业务持久化由 Native 应用负责；没有 profile、持久性或离屏选项。Windows 临时上下文仍使用磁盘目录并在关闭后清理，不承诺纯内存运行。
- 接口消融和契约见 [resources/atomic-framework-api.md](resources/atomic-framework-api.md)；应用接线见 `app/desktop/src/nativeMain/kotlin/dev/kauri/host/Main.kt`。旧的 `runDesktopApplication` 与原宿主实现已删除，不保留兼容启动层。

## 构建

构建在 `stream@macbook:~/ACodeSpace/demo/Kauri` 进行。不要在用户的 Linux 工作站运行 Gradle。

```bash
# 在仓库根目录同步源码；不覆盖远程构建缓存。
rsync -az --exclude=.git --exclude=.gradle --exclude=build --exclude=.agents \
  ./ stream@macbook:ACodeSpace/demo/Kauri/

ssh stream@macbook \
  'cd ~/ACodeSpace/demo/Kauri && bash scripts/prepare-native-mac.sh && bash scripts/release-all-mac.sh'
```

- `scripts/gradle-mac.sh` 从 wrapper 读取 Gradle 版本，显式复用当前用户/Gradle home 下的空闲 daemon JVM，最多两个 worker；身份门禁也覆盖配置缓存命中。
- 没有 daemon 时才使用显式 JDK 初始化；JDK 只运行 Gradle，不进入应用。
- 使用 nvm 管理的 Node 24.12.0（`.nvmrc`），Gradle 不再重复下载 Node。
- 版本集中在 `gradle/libs.versions.toml`，原生工具链兼容描述位于 `gradle/native-toolchains.properties`；保留已有版本，未借治理升级应用依赖。
- 根构建只保留一个 KMP `apply false` 共享插件类，避免实测出现的 KGP build-service 类加载冲突；没有 `base` 或根 `check` / `assemble` 包装。模块直接声明实际使用的插件。
- `prepare-native-mac.sh` 是显式的原生依赖准备：使用 KGP 的分发下载任务和 Native 自带的依赖检查安装匹配工具链，校验 Linux GTK/WebKitGTK SDK 下载并准备链接输入。它不编译 C/C++ 宿主或静态包装库；可用 `macosArm64` 参数只准备 Mac，普通 Gradle 构建不重复执行它。
- 原生下载缓存在用户 `Library/Caches/Kauri/native`，可通过 `KAURI_NATIVE_CACHE` 改位置；准备结果在 `build/native-deps`。更改 SDK 锁定描述或删除该目录后，需要重新准备。修改 Kotlin 宿主源码无需重建包装库。Linux SDK 是构建输入，不打包进应用。
- cinterop 从仓库公共 C 头生成绑定，不读取已准备的 `.a` 或 SDK；这些文件只在最终链接时需要。因此 IDE 导入无需先复制 Mac 的原生构建输出。
- settings 集中管理 Gradle 仓库；Binaryen 保留受限 Ivy 端点。版本不升级，严格配置缓存和资源预算不变。
- 默认任务构建 production Wasm 和 Native；额外接线仅使用标准 `Exec` 打包前端、生成四平台资源 object。没有自写编译器发现、Native 编译任务或发布任务类。
- Windows 由 Kotlin/Native 在 Microsoft EdgeWebView 安装目录中发现并以受限 `LoadLibraryExW` 搜索路径加载运行时，调用已接受兼容风险的五参数内部入口；运行时模块按绝对路径缓存至进程退出，避免回调栈上的卸载竞态。不维护 C/C++ loader 或旁置 loader DLL。Linux 直接调用 GTK/WebKitGTK，缺库可能在进入 `main` 前失败。
- `release-all-mac.sh` 调用标准 release link、运行非 GUI Native/资源测试，再复制产物、对 Mac strip / ad-hoc 签名并执行 CLI/HTTP 检查。GUI smoke 只在显式选择自有图形 runner 时执行，默认收据标注未运行。
- `release-mac.sh` 复用同一流程，只构建 Mac 目标；两者使用同一输出路径。发布和 smoke 全程持有仓库锁，状态位于不会被 `clean` 删除的 `.gradle/kauri/`。
- 构建约束见 [checklist/build.md](checklist/build.md)；本轮收敛与审查见 [resources/minimal-static-host.md](resources/minimal-static-host.md)，早期治理记录见 [resources/build-review.md](resources/build-review.md)。
- 根入口清理及实际 IDEA 导入修复/验证见 [resources/root-idea-sync.md](resources/root-idea-sync.md)；不用在编辑机器准备 Mac 链接 SDK。

## 运行

构建输出（每个目录中的可执行文件都可单独复制）：

```text
build/release/
├── macosArm64/Kauri
├── mingwX64/Kauri.exe
├── linuxX64/Kauri
├── linuxArm64/Kauri
├── SHA256SUMS
└── VALIDATION.txt
```

例如在 Mac 上直接运行：

```bash
~/ACodeSpace/demo/Kauri/build/release/macosArm64/Kauri
```

Linux 上对匹配架构的文件执行 `chmod +x Kauri && ./Kauri`；Windows 上运行 `Kauri.exe`。运行不需要 Gradle、JDK、Node.js 或仓库源码。

`SHA256SUMS` 是完整性记录；`VALIDATION.txt` 是绑定特定 hash 的运行检查收据，使用旧收据前应比对当前产物。Mac-only 发布会移除可能过时的四目标 manifest，保留目标目录内的校验文件。

**单文件交付不等于自带浏览器引擎。**

| 目标 | 系统运行依赖 |
| --- | --- |
| macosArm64 | 支持 WasmGC 的系统 WKWebView；本次构建主机 macOS 26.5.1 |
| mingwX64 | Windows 10/11 x64 + 支持 WasmGC 的现代 Microsoft Edge WebView2 Runtime |
| linuxX64 | glibc x86_64、`libgcc_s.so.1`、`libcrypt.so.1`、GTK 3、支持 WasmGC 的 WebKitGTK 4.1、图形会话 |
| linuxArm64 | glibc aarch64 + 同上 |

- Linux 缺依赖由系统动态加载器或 GTK/WebKit 报错，不降级成外部浏览器。Debian/Ubuntu 常见包名是 `libgtk-3-0`（或 t64 版本）与 `libwebkit2gtk-4.1-0`；较旧 WebKitGTK 不一定支持所需 Wasm 特性。
- 系统 WebView 管理会话内的浏览器状态：Mac/GTK 使用非持久上下文，Windows 使用独立临时目录；不跨次启动恢复浏览器状态，也不删除旧 profile。应用笔记仍由 Native 正常持久化。这不需要旁置应用资源文件。
- Mac ad-hoc 签名不是 Developer ID 公证；Windows 产物未做 Authenticode 签名。

## 测试与证据

```bash
# 在 Mac 仓库根目录运行。
bash scripts/gradle-mac.sh :app:shared:macosArm64Test \
  :app:desktop:macosArm64Test :framework:macosArm64Test
bash scripts/test-build-policy.sh
# 下列 GUI 命令会显示正常窗口，只能在独占的图形 runner 上使用。
KAURI_ISOLATED_DISPLAY=1 bash scripts/smoke-mac.sh build/release/macosArm64/Kauri
# 以下仅供 Linux 目标机器上的自有图形 runner；本轮未执行。
KAURI_ISOLATED_DISPLAY=1 bash scripts/smoke-linux.sh build/release/linuxX64/Kauri
```

- Mac 冒烟入口把**唯一一个二进制**复制到临时空目录，用正常系统窗口验证 Compose Wasm、Native 系统信息、笔记、Flow、RPC 与原生导航策略。
- GUI 测试使用临时业务目录和非持久化/临时浏览器数据；`KAURI_ISOLATED_DISPLAY=1` 只声明 runner 归属，不创建隔离。没有自有图形环境时不运行，不在共享桌面尝试隐藏窗口。
- 已删除测试专用的 frame-clock 替换，统一使用浏览器原生 frame clock。
- 本轮没有独占图形 runner，GUI 与 Cocoa 故障回调清理尚未回归。`build/smoke/` 的旧截图不是新产物证据；Compose 自带 UI 测试也不能替代原生宿主生命周期验证。
- 编译成功不等于目标平台已运行。本次实际验证、尺寸、校验值和缺失 runner 记录见 [resources/validation.md](resources/validation.md)。
- `Kauri --licenses` 可打印嵌入的第三方说明，不需要打开窗口。
- 原子能力重做、独立审查处置与故障注入边界见 [resources/atomic-rewrite.md](resources/atomic-rewrite.md)。
- 当前离屏删除范围、产物与未运行的 GUI 门禁见 [resources/offscreen-ablation.md](resources/offscreen-ablation.md)。
- 固定临时浏览器上下文、两参数窗口接口及当前验证见 [resources/ephemeral-browser-context.md](resources/ephemeral-browser-context.md)。

## 仓库导航

| 路径 | 职责 |
| --- | --- |
| `app/shared/` | 应用 `@Rpc` 契约、DTO 和校验 |
| `app/desktop/` | 应用入口、服务实现、原生业务 API 和存储测试 |
| `app/frontend/` | Compose Wasm UI 和生成的 kRPC client |
| `framework/` | 原子窗口/WebView、嵌入资源读取及其平台适配 |
| `gradle/` | 版本目录、原生工具链/SDK 锁定描述、wrapper |
| `scripts/` | 远程 Mac 交叉构建、release 与自有 runner 的 GUI smoke |
| `checklist/` | 已确认的架构边界与验证门槛 |
| `kanban/` | 可恢复的 Task Tree；`Draft.md` 是用户草稿区 |
| `resources/` | 上游来源、版本发现和实测结果 |
| `.agents/skills/` | 四个上下文工作流 Git 子模块 |

使用 `git submodule update --init --recursive` 恢复上下文工作流。平台宿主不再依赖第三方 `webview/webview` 子模块。

## 实验边界

- “开发体验优于 Tauri”“体积小于 Electron / Compose Desktop”目前是待比较的假设，不是实测结论。
- 不携带 Chromium / JVM 不等于没有成本：Compose 与 Skia 的 Wasm 资源会增加二进制体积，系统 WebView 也会创建辅助进程。
- 尚未实现自动更新、安装器、平台签名 / 公证或生产级权限策略；这是可运行性实验，不是成熟的 Tauri 替代品。
