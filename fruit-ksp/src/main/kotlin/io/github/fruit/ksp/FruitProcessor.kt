package io.github.fruit.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class FruitProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val processedQualifiedNames = mutableSetOf<String>()
    private val classesToRegister = mutableListOf<KSClassDeclaration>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("io.github.fruit.annotations.Pick")
        
        val classesFromProperties = symbols
            .filterIsInstance<KSPropertyDeclaration>()
            .mapNotNull { it.parentDeclaration as? KSClassDeclaration }
        
        val classesFromClassAnnotations = symbols
            .filterIsInstance<KSClassDeclaration>()

        val classesInThisRound = (classesFromProperties + classesFromClassAnnotations).toSet()
        
        classesInThisRound.forEach { clazz ->
            val qualifiedName = clazz.qualifiedName?.asString() ?: return@forEach
            if (qualifiedName !in processedQualifiedNames) {
                generateAdapter(clazz)
                processedQualifiedNames.add(qualifiedName)
                classesToRegister.add(clazz)
            }
        }
        
        return emptyList()
    }

    override fun finish() {
        if (classesToRegister.isEmpty()) return
        generateRegistry()
    }

    private fun generateAdapter(classDeclaration: KSClassDeclaration) {
        val packageName = classDeclaration.packageName.asString()
        val fullName = getFullSimpleName(classDeclaration)
        val adapterName = "${fullName}PickAdapter"

        val fileSpec = FileSpec.builder(packageName, adapterName)
            .addImport("io.github.fruit.annotations", "Pick")
            .addImport("io.github.fruit.annotations", "Attrs")
            .addType(
                TypeSpec.classBuilder(adapterName)
                    .addSuperinterface(
                        ClassName("io.github.fruit", "PickAdapter")
                            .parameterizedBy(classDeclaration.toClassName())
                    )
                    .addFunction(
                        FunSpec.builder("read")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("element", ClassName("com.fleeksoft.ksoup.nodes", "Element"))
                            .addParameter("css", String::class.asTypeName())
                            .addParameter("attr", String::class.asTypeName())
                            .addParameter("ownText", Boolean::class.asTypeName())
                            .returns(classDeclaration.toClassName().copy(nullable = true))
                            .addCode(generateReadLogic(classDeclaration))
                            .build()
                    )
                    .build()
            )
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(false, classDeclaration.containingFile!!))
    }

    private fun getFullSimpleName(classDeclaration: KSClassDeclaration): String {
        val names = mutableListOf<String>()
        var current: KSDeclaration? = classDeclaration
        while (current is KSClassDeclaration) {
            names.add(0, current.simpleName.asString())
            current = current.parentDeclaration
        }
        return names.joinToString("_")
    }

    private fun generateReadLogic(classDeclaration: KSClassDeclaration): CodeBlock {
        val builder = CodeBlock.builder()
        val className = classDeclaration.toClassName()
        val isKotlin = classDeclaration.origin == Origin.KOTLIN
        
        builder.addStatement("var currentElement = element")
        
        val classPick = classDeclaration.annotations.find { 
            it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.github.fruit.annotations.Pick" 
        }
        if (classPick != null) {
            val cssValue = classPick.arguments.find { it.name?.asString() == "value" }?.value as? String ?: ""
            if (cssValue.isNotEmpty()) {
                builder.addStatement("currentElement = element.selectFirst(%S) ?: return null", cssValue)
            }
        }

        if (isKotlin) {
            // Kotlin 类使用具名参数构造函数
            builder.add("return %T(\n", className)
            builder.indent()
            classDeclaration.getAllProperties().forEach { prop ->
                val pickAnnotation = prop.annotations.find { 
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.github.fruit.annotations.Pick" 
                }
                if (pickAnnotation != null) {
                    val args = pickAnnotation.arguments
                    val cssValue = args.find { it.name?.asString() == "value" }?.value as? String ?: ""
                    val attr = args.find { it.name?.asString() == "attr" }?.value as? String ?: "text"
                    val ownText = args.find { it.name?.asString() == "ownText" }?.value as? Boolean ?: false
                    
                    val type = prop.type.resolve()
                    val propertyName = prop.simpleName.asString()
                    
                    builder.add("%N = ", propertyName)
                    generateReadForType(builder, type, cssValue, attr, ownText)
                    builder.add(",\n")
                }
            }
            builder.unindent()
            builder.addStatement(")")
        } else {
            // Java 类：使用无参构造函数并尝试通过反射或公开字段赋值（由于是无反射路径，这里需要权衡）
            // 针对 V2compose，我们支持通过 Field 赋值，但在 KSP 阶段如果字段是私有的，我们需要反射或者 Setter。
            // 为了“彻底”路径，建议用户将 Bean 转为 Kotlin。但在现有 Java 项目中，我们生成 Setter 调用逻辑。
            builder.addStatement("val instance = %T()", className)
            classDeclaration.getAllProperties().forEach { prop ->
                val pickAnnotation = prop.annotations.find { 
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.github.fruit.annotations.Pick" 
                }
                if (pickAnnotation != null) {
                    val args = pickAnnotation.arguments
                    val cssValue = args.find { it.name?.asString() == "value" }?.value as? String ?: ""
                    val attr = args.find { it.name?.asString() == "attr" }?.value as? String ?: "text"
                    val ownText = args.find { it.name?.asString() == "ownText" }?.value as? Boolean ?: false
                    
                    val type = prop.type.resolve()
                    val propertyName = prop.simpleName.asString()
                    
                    // 这里假设 Java Bean 有公共字段或符合标准的 Kotlin 属性映射
                    builder.add("instance.%N = ", propertyName)
                    generateReadForType(builder, type, cssValue, attr, ownText)
                    builder.add("\n")
                }
            }
            builder.addStatement("return instance")
        }
        
        return builder.build()
    }

    private fun generateReadForType(builder: CodeBlock.Builder, type: KSType, css: String, attr: String, ownText: Boolean) {
        val qualifiedName = type.declaration.qualifiedName?.asString() ?: ""

        when {
            qualifiedName == "kotlin.String" -> builder.add("io.github.fruit.bind.BasicPickAdapters.STRING_ADAPTER.read(currentElement, %S, %S, %L) ?: \"\"", css, attr, ownText)
            qualifiedName == "kotlin.Int" -> builder.add("io.github.fruit.bind.BasicPickAdapters.INT_ADAPTER.read(currentElement, %S, %S, %L) ?: 0", css, attr, ownText)
            qualifiedName == "kotlin.Long" -> builder.add("io.github.fruit.bind.BasicPickAdapters.LONG_ADAPTER.read(currentElement, %S, %S, %L) ?: 0L", css, attr, ownText)
            qualifiedName == "kotlin.Float" -> builder.add("io.github.fruit.bind.BasicPickAdapters.FLOAT_ADAPTER.read(currentElement, %S, %S, %L) ?: 0.0f", css, attr, ownText)
            qualifiedName == "kotlin.Boolean" -> builder.add("io.github.fruit.bind.BasicPickAdapters.BOOLEAN_ADAPTER.read(currentElement, %S, %S, %L) ?: false", css, attr, ownText)
            qualifiedName == "kotlin.collections.List" || qualifiedName == "kotlin.collections.MutableList" || qualifiedName == "java.util.List" -> {
                val elementType = type.arguments.firstOrNull()?.type?.resolve()
                if (elementType != null) {
                    builder.add("currentElement.select(%S).map { ", css)
                    generateReadForType(builder, elementType, "", attr, ownText)
                    builder.add(" }")
                } else {
                    builder.add("emptyList()")
                }
            }
            else -> {
                val nestedClass = type.declaration as? KSClassDeclaration
                val nestedFullName = if (nestedClass != null) getFullSimpleName(nestedClass) else type.declaration.simpleName.asString()
                val adapterName = "${nestedFullName}PickAdapter"
                // 递归调用
                builder.add("%T().read(currentElement.selectFirst(%S) ?: currentElement, \"\", \"text\", false)!!", 
                    ClassName(type.declaration.packageName.asString(), adapterName), css)
            }
        }
    }

    private fun generateRegistry() {
        val fileSpec = FileSpec.builder("io.github.fruit", "FruitRegistry")
            .addFunction(
                FunSpec.builder("registerGeneratedAdapters")
                    .receiver(ClassName("io.github.fruit", "Fruit"))
                    .addCode(CodeBlock.builder().apply {
                        classesToRegister.forEach { clazz ->
                            val packageName = clazz.packageName.asString()
                            val fullName = getFullSimpleName(clazz)
                            val adapterClassName = ClassName(packageName, "${fullName}PickAdapter")
                            addStatement("registerAdapter(%T::class, %T())", clazz.toClassName(), adapterClassName)
                        }
                    }.build())
                    .build()
            )
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(false, *classesToRegister.mapNotNull { it.containingFile }.toTypedArray()))
    }
}

class FruitProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FruitProcessor(environment.codeGenerator, environment.logger)
    }
}
