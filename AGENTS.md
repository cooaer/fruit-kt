# Fruit-KT: AI 代理上下文与指南

`fruit-kt` 是一个 Kotlin Multiplatform (KMP) HTML 到对象的绑定库。它是原 Java 版 `Fruit` 库的现代化、无反射移植版本，旨在无缝运行于 Android、iOS（通过 Compose Multiplatform）以及其他 Kotlin 支持的平台。

## 交互协议
- **语言**: 除非特别要求，否则使用中文回复。
- **代码提交**: 每当完成重要的代码修改或功能实现后，请主动执行 `git commit` 保存进度。

## 核心使命
通过 CSS 选择器提供一种声明式的方法将 HTML 解析为 Kotlin 数据类，同时确保与不支持 JVM 反射的平台（如 iOS/Kotlin Native）的兼容性。

## 架构决策

### 1. KSP vs. 反射
与原 Java 版本不同，`fruit-kt` **避免使用运行时反射**。
- **策略**：使用 **Kotlin Symbol Processing (KSP)** 在编译期扫描 `@Pick` 注解。
- **输出**：KSP 为每个标注的类生成高性能的 `PickAdapter` 实现。
- **iOS 兼容性**：这种方法对于 iOS 支持至关重要，因为 Kotlin/Native 不支持 `java.lang.reflect`。

### 2. HTML 解析引擎
- **引擎**：[Ksoup](https://github.com/fleeksoft/ksoup)（Jsoup 的 KMP 移植版）。
- **选择器**：支持标准的 CSS 选择器（如 `.title`, `#author`, `div > span`）。

### 3. 模块结构
- **根目录 (`:`)**：包含通用多平台逻辑、注解 (`@Pick`) 和 `Fruit` 入口点。
- **`:fruit-ksp`**：代码生成器。生成 `*PickAdapter` 类和全局 `registerGeneratedAdapters()` 扩展函数。
- **`:fruit-converter-retrofit`**：针对 Android 的 Retrofit 适配器。

## 核心 API 与用法

### 注解
- **`@Pick(value: String, attr: String, ownText: Boolean)`**：
    - `value`：CSS 选择器。
    - `attr`：目标属性（默认为 `text`）。可使用 `Attrs.HREF`, `Attrs.SRC` 等。
    - `ownText`：如果为 true，则仅获取元素的直接文本（不包括子元素）。

### 集成方式
1. **初始化**：
   ```kotlin
   val fruit = Fruit().apply { registerGeneratedAdapters() }
   ```
2. **解析**：
   ```kotlin
   val news = fruit.fromHtml(html, NewsInfo::class)
   ```

## 代理开发指南

- **添加类型**：在支持新字段类型时，请更新 `commonMain` 中的 `BasicPickAdapters` 和 `FruitProcessor.kt` 中的 `generateReadForType` 逻辑。
- **KSP 生成**：生成代码位于 `build/generated/ksp` 目录。如果 IDE 中符号缺失，建议运行 `./gradlew build`。
- **包名完整性**：始终使用 `io.github.fruit` 包名。
- **测试**：在 `commonTest` 中编写跨平台逻辑测试。如果测试环境下 KSP 未运行，请模拟生成的适配器。

## 未来会话背景
- **创建日期**：2026年3月19日。
- **状态**：已实现核心 KSP 引擎、Ktor 支持和 Retrofit 支持。
- **关键文件**：
    - `Fruit.kt`：主要 API。
    - `FruitProcessor.kt`：生成逻辑。
    - `BasicPickAdapters.kt`：基本类型处理。
