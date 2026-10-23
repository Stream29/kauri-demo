# 统一静态 webview：隔离验证

日期：2026-10-21。测试固定的 webview 0.12.0（`3ab4b5d722438fc8a13e6ca830c5e2372d19a01d`），未替换生产宿主或改变架构决策。

本页记录当时的隔离测试。后续已按用户授权迁移，当前实现见 [minimal-static-host.md](minimal-static-host.md)；不能将旧探针产物当作新 release。

## 结论

- **四平台 webview 静态库及 Kotlin/Native C API 消费者均已编译、链接成功。**
- **Mac 已完成真正的单文件应用集成**：同一可执行文件内包含现有 Native 后端、嵌入的 Compose Wasm、typed kRPC 与静态 webview；不是另起服务配一个浏览器窗口。
- 统一库可以接管窗口/WebView 基础实现，但现有 C API 不直接覆盖所有安全与测试配置；本次 Mac 验证仍使用少量平台代码。
- 预编译 `.a` 不会让跨平台链接成本消失：**Linux 消费者仍需要目标 GTK/WebKitGTK 链接 SDK**，而不只是一个 C 头和 `.a`。
- 本轮是可行性测试，不是完成迁移、发布新版本或证明它比现有实现更快/更可靠。
- 后续已收紧一期范围：路由和内容内对话框归 Compose Wasm；profile/离屏的最小职责与创建期约束见 [最小宿主分析](minimal-host-policy.md)。

## 实际覆盖

| 目标 | 静态库 | Kotlin 消费者 | 运行覆盖 |
| --- | ---: | --- | --- |
| macosArm64 | 132,352 字节 | 基础消费者及独立 Kauri 应用探针链接成功 | 基础版本查询；完整离屏 Wasm/kRPC/Canvas；导航/弹窗及后端安全检查 |
| mingwX64 | 286,234 字节 | PE x64 控制台探针链接成功 | 未在 Windows 执行；不是 Windows GUI 版 Kauri 已通过 |
| linuxX64 | 150,124 字节 | ELF x64 探针链接成功 | 关闭 DISPLAY/WAYLAND_DISPLAY 后查询版本成功；`LD_BIND_NOW=1` 也通过；未调用 GTK 创建窗口 |
| linuxArm64 | 149,224 字节 | ELF aarch64 探针链接成功 | 未执行 |

- Windows 导入表只有 Windows 系统 DLL；未导入 WebView2Loader、libstdc++、libgcc 或其他伴随 DLL。此结果不验证实际运行时发现/创建 WebView2。
- Linux ELF 直接依赖 GTK、WebKitGTK、GLib 等系统共享库；本次静态链接的是包装层，不是系统浏览器引擎。与现有 `dlopen` 宿主相比，这些库会在进入 `main` 前成为必需依赖。
- Mac 应用探针 strip / ad-hoc 签名后为 **17,030,656 字节（约 16.24 MiB）**，系统 dylib/framework 仍外置。不同入口与测试配置不构成严格的增量体积比较。

## Mac 应用验证

- 复用现有已编译的 `app/shared`、`app/desktop`、`framework` klib 与 `frontend.o`，只新编译隔离测试入口并静态链接 webview；没有重建或重写业务实现。
- 测试仅将一个可执行文件复制到临时空目录运行；资源从该文件提供，笔记使用新建的临时目录。
- 真实 Compose Canvas 采样得到 110 种颜色；Native 系统信息、Unicode 笔记往返、超长拒绝和 Flow 1/2/3 通过，无权限 RPC 返回 403。
- `--smoke` 运行中，原生导航与 popup 回调分别记录一次拒绝。
- 同一二进制另以 `--serve` 无窗口启动，由既有安全脚本验证健康、Origin、Host、WebSocket Origin、错误 capability、两种路径穿越共 7 项；这些是独立的后端回归检查，不是 webview 自身提供的过滤能力。
- 第一次策略测试选用了浏览器保留端口 9，未观察到导航回调而超时；修正为本次服务端口上的 `localhost`（不同于应用 origin 的 `127.0.0.1`）后通过。没有把这个探针问题当作库故障。
- 窗口从创建起就位于屏外，通过 caller-owned window 传给 `webview_create`；未先显示再隐藏，没有激活用户窗口。
- 没有执行库自行创建可见顶层窗口的路径，也没有做正常前台开关窗口验收；本次运行覆盖的是调用方窗口内的 WebView 与事件循环。
- 通过 `webview_get_native_handle` 获取 WKWebView，补充导航/UIDelegate、防 popup、后台调度偏好及结果采集。成功的是“统一库 + 平台策略”，不是仅靠公共 C API 的零平台代码方案。

## 构建成本

- 所有 C++、cinterop 和 Kotlin 编译均在 Mac 串行、降低优先级执行。仅为读取既有依赖图运行一次 Gradle，通过项目 helper 检测/复用 daemon JVM；没有本机 Gradle 或 Linux 编译。
- Mac 使用 Xcode 编译器与系统 framework。Windows 使用现有 Konan LLVM/MinGW 和已校验的 WebView2 SDK 头文件，采用上游的 MinGW 兼容头和内置 loader。
- Linux 从 Debian bookworm 的官方索引提取每架构 37 个包，逐包核对索引中的 SHA-256，形成约 164/165 MiB 的临时开发目录；没有安装系统软件包。这些 SDK 文件不嵌入最终程序。
- Linux wrapper 编译使用 SDK 头文件与既有 GCC 8.3 sysroot；Kotlin 消费者最终链接还需要 SDK 的真实 GTK/WebKitGTK `.so` 输入。GNU libstdc++ 静态归入探针。
- Linux 链接允许共享库的传递依赖在目标系统解析（`--allow-shlib-undefined`）。x64 的版本路径及立即符号绑定只覆盖当前测试机，不证明 GTK 窗口、后续动态模块或其他目标的运行条件；arm64 仍为链接证据。
- 如果应用构建仍统一在 Mac，预编译静态库之外还需提供目标系统链接输入/相应 SDK 元数据；当前只生成本地实验产物，未建设制品发布服务。

## 不能直接切换的部分

- **安全配置**：公共 API 没有完整的导航、popup 和 profile 配置入口。Mac hook 已测；Windows/Linux 的同等策略、回调生命周期与退出仍需目标系统测试。
- **上游默认行为**：当前 Cocoa 实现启用 JS clipboard/paste 偏好，并提供文件选择面板 delegate；本次前端未使用这些功能，测试没有证明权限与原宿主完全等价。
- **代理覆盖**：测试的 UIDelegate 替换了上游文件面板处理。定版时需明确转发还是拒绝该行为，并防范上游代理职责变化；不能把一次 smoke 通过视为全部代理行为等价。
- **Windows loader**：内置 loader 仍使用当前已知的内部入口，并尝试加载 loader DLL。迁移时要保留既有 DLL 搜索硬化，不能把“未导入 DLL”理解为“不会尝试动态加载”。
- **Linux 启动行为**：直接的 GUI 共享库依赖改变了缺库诊断，以及 `--licenses` / headless 模式可能不依赖 GUI 库的既有行为；需要决定是否接受。
- **离屏与 profile**：未修改上游源码即可完成 Mac 本次测试，但仍需平台句柄扩展；不能据此承诺所有系统的 profile、权限和离屏行为都已覆盖。

## 证据与产物

- 本机与 Mac：`build/static-webview-spike/`，包含四个静态库、基础消费者、Mac 应用探针、校验清单、上游头文件/许可证和 Mac 截图。
- Mac 的 `StaticKauri` 是测试入口，只支持 `--smoke` / `--serve`；不是替换现有 release 的普通桌面入口。基础消费者无参数只查询版本，未执行显示窗口选项。
- `build/review/static-webview-*` 保存源码快照、编译、SDK 索引摘要、依赖审计及运行日志。原始 capability 文件和临时笔记不保留。
- 本机/Mac 临时探针和 SDK 已删除；项目锁已释放，仅关闭本轮创建并确认空闲的 Gradle daemon。
- 生产 `build/release/SHA256SUMS` 重新校验通过。本轮没有创建 Git commit。

## 来源

- 上游 [静态库构建目标](https://github.com/webview/webview/blob/0.12.0/core/CMakeLists.txt) 与 [依赖/编译选项](https://github.com/webview/webview/blob/0.12.0/README.md)；隔离命令编译同一个 `core/src/webview.cc`，不修改参考子模块。
- 上游 [C API 与实现](https://github.com/webview/webview/blob/0.12.0/core/include/webview/webview.h) 支持 caller-owned window / native handle；配置与策略能力根据该固定版本源码核对。
- Debian [WebKitGTK 开发包](https://packages.debian.org/bookworm/libwebkit2gtk-4.1-dev) 与 [GTK 3 开发包](https://packages.debian.org/bookworm/libgtk-3-dev)；实际版本和 SHA-256 以归档的下载索引摘要为准。

## 独立复核

- agy / Gemini 3.8 Flash 完成只读审查，采纳了分清 `--smoke` / `--serve`、强调编译/消费两端 SDK 需求及代理覆盖影响的建议。
- 未采纳“UIDelegate 覆盖必然破坏 JS 分发、标题和窗口生命周期”的定性：固定版本源码中这些职责并非都在 UIDelegate，其文件面板行为才是本次明确替换的部分。风险按具体职责记录，而不是扩张为未证实故障。
- 原始复核在忽略目录 `build/review/agy-static-webview-spike.json`，不替代目标平台运行验证。
