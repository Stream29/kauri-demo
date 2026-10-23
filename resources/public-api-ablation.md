# 应用接入 API 消融

状态：以下是上一轮单入口实现的历史记录。该入口现已删除，按用户确认的原子能力边界重做；当前实现与验证见 `resources/atomic-rewrite.md`，不可用本文旧签名或收据代表当前交付。

## 使用方式

```kotlin
fun main(args: Array<String>) = runDesktopApplication(
    args,
    name = "Kauri",
    service = DesktopService::class,
    createService = ::NativeService,
)
```

- `title` 可选，默认使用 `name`。工厂类型是 `(dataDirectory: String) -> Service`，示例 `NativeService` 的构造参数正是该目录；每次应用运行只创建一个共享实例，不是每条 WebSocket 重新创建。
- `service` 是准确的 `@Rpc` 接口；实际负例编译已确认传实现类 `NativeService::class` 会被 kRPC 插件拒绝。
- 前端继续使用同一份共享接口和 typed kRPC，应用业务不进入 framework。

## 真正去掉的调用层

- 原来：应用调用入口，构造 `DesktopApplication`，实现 `createServer`，构造 `LocalServer`，接收 Route/path，再调用 `rpc/registerService`。
- 现在：一个非 inline 的启动函数，显式契约和一个服务工厂。不是在旧对象链上新增 facade；`DesktopApplication` 已删除，外部 server factory 和路由回调也已删除。
- `LocalServer` 留作内部生命周期实现，自己注册受保护的 RPC 路由。资源、capability、路径校验、notices 和平台辅助代码均为 internal；本轮减少的是公开组装协议和调用深度，不是删掉底层服务或安全逻辑。
- 应用入口的 framework import 从 3 个变为 1 个，Ktor server import 从 1 个变为 0 个；手写公开源符号从 8 个收为 1 个入口，不计自动生成的 SDK 绑定。
- Ktor/kRPC server 依赖改为 implementation。没有 Builder、配置对象、兼容重载或通用服务注册表；当前明确支持单个服务契约。应用自己的原子写入临时名和 Windows 目录创建不再调用框架的公共平台工具。

## 实际验证与边界

- 四目标应用编译通过；四目标 release 重新链接，56s / 91 tasks；同组任务重复 608ms，四个 link up-to-date、严格配置缓存复用。这些不是性能收益测量。
- Native 8 项、资源 4/4、脚本策略 11/11 通过；Mac 单文件普通离屏启动/关闭/finally、真实 Canvas/kRPC/Flow、输入校验、防护及七项 HTTP 安全检查通过。Mac `--licenses` 也重新执行通过。
- 普通名称/标题校验在启动的第一步执行。框架单测只测试其纯校验函数；实际入口由独立二进制回归覆盖，避免单测链接应用资源或提供假资源。
- 首次编译发现遗漏的应用 Windows 工具依赖；应用改用私有目录创建实现。另一次单测链接暴露完整启动器对资源符号的依赖，按上述测试边界修正；失败日志和最终通过日志均保留。
- agy 的初次设计审查误称 `@Rpc` 不能标注类型参数；固定版 0.10.4 的源代码明确允许，实际四目标编译和负例检查支持保留该约束。未来 Closeable/多服务扩展不在本轮添加。
- 源码摘要、负例 fixture、审查及构建/运行日志在忽略目录 `build/review/public-api/`；当前产物见 [验证记录](validation.md)。Windows/Linux 仍只有编译/链接覆盖，不宣称目标 GUI 运行通过。
