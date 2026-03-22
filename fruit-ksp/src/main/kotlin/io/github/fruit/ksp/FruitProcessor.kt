package io.github.fruit.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class FruitProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val processedQualifiedNames = mutableSetOf<String>()
    private val classesToRegister = mutableListOf<KSClassDeclaration>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classesInThisRound =
            resolver.getSymbolsWithAnnotation("io.github.fruit.annotations.Pulp")
                .filterIsInstance<KSClassDeclaration>().toSet()

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
                            .addParameter(
                                "element",
                                ClassName("com.fleeksoft.ksoup.nodes", "Element")
                            )
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

        val classPulp = classDeclaration.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.github.fruit.annotations.Pulp"
        }
        val cssValue = if (classPulp != null) {
            classPulp.arguments.find { it.name?.asString() == "value" }?.value as? String ?: ""
        } else ""

        val elementType = getCollectionElementType(classDeclaration)

        if (elementType != null && cssValue.isNotEmpty()) {
            builder.addStatement("val instance = %T()", className)
            if (classDeclaration.superTypes.any {
                    val qn = it.resolve().declaration.qualifiedName?.asString()
                    qn == "io.github.fruit.converter.retrofit.IBaseWrapper" || qn == "io.github.v2compose.network.bean.IBase"
                }) {
                builder.addStatement("instance.setResponse(element.outerHtml())")
            }

            builder.beginControlFlow("element.select(%S).forEach", cssValue)
            generateReadForType(builder, elementType, "", "text", false, "it")
            builder.add(".let { instance.add(it) }\n")
            builder.endControlFlow()
            builder.addStatement("return instance")
            return builder.build()
        }

        builder.addStatement("var currentElement = element")

        if (cssValue.isNotEmpty()) {
            builder.addStatement(
                "currentElement = element.selectFirst(%S) ?: return null",
                cssValue
            )
        }

        // 统一使用无参构造函数实例化，然后反射设值
        // 这样可以规避 Kotlin val 无法赋值、具名参数禁止以及 Java 私有字段等所有问题
        builder.addStatement("val instance = %T()", className)

        // 自动注入 responseHtml (如果实现了 IBaseWrapper)
        if (classDeclaration.superTypes.any {
                val qn = it.resolve().declaration.qualifiedName?.asString()
                qn == "io.github.fruit.converter.retrofit.IBaseWrapper" || qn == "io.github.v2compose.network.bean.IBase"
            }) {
            builder.addStatement("instance.setResponse(currentElement.outerHtml())")
        }


        classDeclaration.getAllProperties().forEach { prop ->
            val pickAnnotation = prop.annotations.find {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.github.fruit.annotations.Pick"
            }
            if (pickAnnotation != null) {
                val args = pickAnnotation.arguments
                val cssValue = args.find { it.name?.asString() == "value" }?.value as? String ?: ""
                val attr = args.find { it.name?.asString() == "attr" }?.value as? String ?: "text"
                val ownText =
                    args.find { it.name?.asString() == "ownText" }?.value as? Boolean ?: false

                val type = prop.type.resolve()
                val propertyName = prop.simpleName.asString()

                builder.add("instance.%L = ", propertyName)
                generateReadForType(builder, type, cssValue, attr, ownText, "currentElement")
                builder.add("\n")
            }
        }

        builder.addStatement("return instance")
        return builder.build()
    }

    private fun generateReadForType(
        builder: CodeBlock.Builder,
        type: KSType,
        css: String,
        attr: String,
        ownText: Boolean,
        elementName: String
    ) {
        val qualifiedName = type.declaration.qualifiedName?.asString() ?: ""

        when {
            qualifiedName == "kotlin.String" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.STRING_ADAPTER.read(%L, %S, %S, %L) ?: \"\"",
                elementName,
                css,
                attr,
                ownText
            )

            qualifiedName == "kotlin.Int" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.INT_ADAPTER.read(%L, %S, %S, %L) ?: 0",
                elementName,
                css,
                attr,
                ownText
            )

            qualifiedName == "kotlin.Long" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.LONG_ADAPTER.read(%L, %S, %S, %L) ?: 0L",
                elementName,
                css,
                attr,
                ownText
            )

            qualifiedName == "kotlin.Float" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.FLOAT_ADAPTER.read(%L, %S, %S, %L) ?: 0.0f",
                elementName,
                css,
                attr,
                ownText
            )

            qualifiedName == "kotlin.Boolean" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.BOOLEAN_ADAPTER.read(%L, %S, %S, %L) ?: false",
                elementName,
                css,
                attr,
                ownText
            )

            qualifiedName == "kotlin.collections.List" || qualifiedName == "kotlin.collections.MutableList" || qualifiedName == "java.util.List" -> {
                val elementType = type.arguments.firstOrNull()?.type?.resolve()
                if (elementType != null) {
                    builder.add("%L.select(%S).map { ", elementName, css)
                    generateReadForType(builder, elementType, "", attr, ownText, "it")
                    builder.add(" }")
                } else {
                    builder.add("emptyList()")
                }
            }

            else -> {
                val nestedClass = type.declaration as? KSClassDeclaration
                val nestedFullName =
                    if (nestedClass != null) getFullSimpleName(nestedClass) else type.declaration.simpleName.asString()
                val adapterName = "${nestedFullName}PickAdapter"
                
                if (css.isNotEmpty()) {
                    builder.add(
                        "%T().read(%L.selectFirst(%S) ?: %L, \"\", \"text\", false)!!",
                        ClassName(type.declaration.packageName.asString(), adapterName), elementName, css, elementName
                    )
                } else {
                    builder.add(
                        "%T().read(%L, \"\", \"text\", false)!!",
                        ClassName(type.declaration.packageName.asString(), adapterName), elementName
                    )
                }
            }
        }
    }

    private fun getCollectionElementType(classDeclaration: KSClassDeclaration): KSType? {
        val collectionNames = setOf(
            "kotlin.collections.List", "kotlin.collections.MutableList", "java.util.List",
            "kotlin.collections.Collection", "java.util.Collection",
            "kotlin.collections.ArrayList", "java.util.ArrayList"
        )
        classDeclaration.superTypes.forEach { ref ->
            val type = ref.resolve()
            if (collectionNames.contains(type.declaration.qualifiedName?.asString())) {
                return type.arguments.firstOrNull()?.type?.resolve()
            }
            val decl = type.declaration
            if (decl is KSClassDeclaration) {
                val found = getCollectionElementType(decl)
                if (found != null) return found
            }
        }
        return null
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
                            addStatement(
                                "registerAdapter(%T::class, %T())",
                                clazz.toClassName(),
                                adapterClassName
                            )
                        }
                    }.build())
                    .build()
            )
            .build()

        fileSpec.writeTo(
            codeGenerator,
            Dependencies(false, *classesToRegister.mapNotNull { it.containingFile }.toTypedArray())
        )
    }
}

class FruitProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FruitProcessor(environment.codeGenerator, environment.logger)
    }
}
