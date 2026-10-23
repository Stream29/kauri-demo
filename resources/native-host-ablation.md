# 直接 Native 宿主消融

## 范围与实际删除

- 基线是 Session 1034 修复交付；不改业务、平台矩阵、公开配置或构建任务。
- Windows 删除五个只写不读的 handler 指针字段；分配记录仍集中在 `handlers`，销毁仍由 COM 最终 Release 决定，引用计数代码不变。
- 删除 `HandlerKind → IID` 映射，直接保存官方 SDK 的静态 IID；GUID 比较不再临时分配、逐字段复制。
- 五套分配接线收敛为一个私有 `newHandler`：调用处仍分配各自的 SDK vtable、设置类型化 `Invoke`，公共接线只填 SDK `IUnknownVtbl` 前缀和管理 StableRef。不是通用 COM 工厂，也没有手写四槽函数表。依据见 [COM 接口布局](https://learn.microsoft.com/en-us/windows/win32/com/interface-pointers-and-interfaces)。
- GTK 窗口指针、关闭标志只留在回调状态中；删除重复构造属性和三份一次性函数指针别名。Mac 内联单调用 getter，将仅初始化使用的 offscreen 值改为局部变量。删除四行未使用的 GTK typedef/空行及公共宿主无用的 cinterop opt-in/import。

| Kotlin 宿主文件 | 基线行数 | 消融后 |
| --- | ---: | ---: |
| mingwMain/DesktopHost.kt | 651 | 569 |
| linuxMain/DesktopHost.kt | 275 | 265 |
| macosArm64Main/DesktopHost.kt | 176 | 174 |
| nativeMain/DesktopHost.kt | 134 | 131 |
| 合计 | 1,236 | 1,139 |

- 宿主净减 **97 行**；framework 全部 Kotlin 文件从 1,645 行降至 1,548 行。行数只是代码量记录，不是可靠性或性能证明。

## 保留与未采纳

- 保留 COM retain/release、StableRef、事件 token 注销、晚到回调泵、退出幂等与 DLL 安全卸载。单窗口不意味着异步引用会同步消失；[引用计数规则](https://learn.microsoft.com/en-us/windows/win32/com/rules-for-managing-reference-counts)仍适用。
- 保留 GTK policy/create 防护、脚本完成回调和 smoke-only 消息桥；不通过删测试或变回 no-op 来减行数。
- 保留 Mac 普通启动/关闭回归及各平台 smoke 隔离代码；不为测试再引入模式对象、通用配置层或平台插件。
- agy Flash Medium 两轮只读审查分别提供删除候选和检查实际 diff；第二轮未发现新增阻塞。这是源码审查，不是 Windows/Linux 运行证明。
- 首轮报告把仍在生产策略和测试中使用的 RESPONSE 常量当成死代码，给出不可直接套用的 `CValue.ptr` 伪代码，并高估部分删除的“零风险”；这些结论未照搬。本轮以实际类型、调用点和回归结果为准。

## 验证与交付

- Mac 复用既有 daemon 45062 / GraalVM JDK 21.0.8、两个 workers；本机没有运行 Gradle，既有 daemon 未停止。
- 四目标 framework 编译及框架测试通过；四目标 release 真实重新链接，54s、91 tasks（17 executed / 74 up-to-date）。重复相同任务 575ms、四个 link up-to-date、严格配置缓存复用；不是消融性能收益测量。
- 8 项 Native 测试重新执行、资源测试 4/4、构建策略 11/11 通过。策略脚本首次被本任务错误包在额外父锁内，触发嵌套锁拒绝；改用标准独立入口后通过，未修改锁实现，失败日志保留。
- Mac 单文件屏外普通初始化/关闭/finally、Canvas 110 色、typed kRPC/Flow、Unicode 笔记、输入校验、文档/新窗口拒绝和七项 HTTP 安全检查通过。
- 当前产物、大小和 hash 见 [验证记录](validation.md)。旧修复收据保存在忽略目录 `build/review/host-ablation/`；该目录也保留 diff、前后行数、审查 JSON 及本轮构建/运行日志。
- Windows/Linux 仍只有交叉编译/链接覆盖，本轮没有关闭它们的 GUI 运行门禁。没有新增通用抽象、C/C++ 宿主、wrapper 或构建任务。
