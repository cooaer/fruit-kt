# 将 `fruit-kt` 重组为标准 KMP 库

## 摘要

采用“分层重组”方案：把仓库整理为一个标准 KMP 核心库加若干可选扩展模块。核心目标是让 `:fruit` 成为主流稳定形态的
Kotlin Multiplatform library，清理当前非标准源码集连接、核心模块内嵌扩展依赖、以及核心模块自应用 KSP
的结构问题；同时保留 `KSP` 和 `Retrofit` 作为独立模块，新增独立的 `Ktor` 扩展模块。

## 关键变更

- 核心模块 `:fruit`
    - 改为 `kotlin("multiplatform") + com.android.library` 组合，移除
      `com.android.kotlin.multiplatform.library`。
    - 标准化源码集为 `commonMain`、`commonTest`、`androidMain`、`androidUnitTest`、`iosMain`、`iosTest`。
    - 去掉当前手写但未正确挂接的 `iosMain` 配置，改用默认 hierarchy template 生成的标准结构。
    - 保留平台范围为 Android + iOS，不额外扩 JVM/JS/Wasm。
    - 仅保留平台无关核心能力：注解、`Fruit` 入口、`SliceAdapter`/`SliceAdapterFactory`、基础类型读取、HTML
      解析核心逻辑。
    - 移除核心模块对 Ktor 的直接依赖与 API 暴露。

- 扩展与工具模块
    - `:fruit-ksp` 保持 JVM/KSP processor 模块定位，只负责生成 `*SliceAdapter` 和注册函数。
    - 新增 `:fruit-ktor`，承载 `FruitContentConverter` 与 `ContentNegotiationConfig.fruit(...)` 扩展，依赖
      `:fruit`，不反向影响核心。
    - 保留 `:fruit-converter-retrofit` 作为 JVM/Android 扩展模块，只依赖 `:fruit`。
    - 清理核心对扩展的反向耦合；`IBaseWrapper` 这类扩展特化接口从核心移除或下沉到对应扩展模块。

- 构建与依赖接入
    - 根工程只负责插件版本、仓库、公共版本对齐，不承载业务逻辑。
    - `:fruit` 不再应用 `com.google.devtools.ksp`；KSP 改为消费侧接入。
    - 调整测试/示例策略，使需要验证代码生成的场景由专门的测试样例模块或 fixture 承担，而不是由核心库自处理生成。
    - 本轮不补齐 Maven Central 细节，但结构需为后续接入 `maven-publish`/Dokka/签名留好入口。

## 对外接口与兼容性变化

- 保留核心 artifact 语义为 `fruit`；允许做整理性破坏，不强求兼容当前接入方式。
- `io.github.fruit.ktor.FruitContentConverter` 及其安装扩展从核心模块迁出到 `:fruit-ktor`。
- 核心库不再内建 KSP 使用方式；使用方需要显式引入 `:fruit-ksp` 作为 KSP processor。
- 如 `IBaseWrapper` 仍被 Retrofit 场景使用，其包位置和依赖归属会变更，不再属于核心公共 API。

## 测试与验收

- 构建验收
    - `./gradlew build` 能通过。
    - `:fruit` 配置阶段不再出现 Kotlin hierarchy template 和 unused source set 警告。
    - 各模块依赖方向正确：扩展依赖核心，核心不依赖扩展。

- 功能验收
    - `commonTest` 覆盖核心 HTML 解析、基础类型读取、嵌套对象、列表、属性读取、`ownText` 行为。
    - `:fruit-ksp` 有生成回归用例，验证 `@Slice`/`@Pick` 仍能生成可编译适配器与注册代码。
    - `:fruit-ktor` 验证 HTML 响应经 Ktor converter 可反序列化为目标对象。
    - `:fruit-converter-retrofit` 验证 Retrofit converter 仍能从 HTML 响应生成对象。

## 实施顺序

1. 重写 `settings.gradle.kts` 与模块清单，加入 `:fruit-ktor`，明确根工程聚合职责。
2. 标准化 `:fruit` 的 Gradle 插件与 source set 结构，迁移测试目录到 `commonTest`/平台测试。
3. 把 Ktor converter 从 `:fruit` 拆到 `:fruit-ktor`，修正 imports 与模块依赖。
4. 清理核心模块中的扩展耦合接口与处理逻辑，必要时把特化行为下沉到扩展模块。
5. 调整 `:fruit-ksp` 与测试样例接入方式，去掉核心模块自应用 KSP 的配置。
6. 修复受影响的测试、示例和构建脚本，确保全仓 `build` 通过且无结构性警告。

## 假设与默认值

- 默认目标是“标准化现有 Android+iOS 核心库”，不是扩平台。
- 默认接受模块边界重整带来的接入破坏，不额外做兼容层。
- 默认保留 `KSP`、`Ktor`、`Retrofit` 三类能力，但作为独立模块分层提供。
- 默认本轮不实现公开仓库发布细节，只把结构整理到可自然接入发布体系。
