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

    private val processedClasses = mutableListOf<KSClassDeclaration>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("io.github.fruit.annotations.Pick")
        val classesToProcess = symbols
            .filterIsInstance<KSPropertyDeclaration>()
            .mapNotNull { it.parentDeclaration as? KSClassDeclaration }
            .toSet()

        classesToProcess.forEach { 
            generateAdapter(it)
            processedClasses.add(it)
        }
        return emptyList()
    }

    override fun finish() {
        if (processedClasses.isEmpty()) return
        generateRegistry()
    }

    private fun generateAdapter(classDeclaration: KSClassDeclaration) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val adapterName = "${className}PickAdapter"

        val fileSpec = FileSpec.builder(packageName, adapterName)
            .addImport("io.github.fruit.annotations", "Pick")
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
                            .addParameter("pick", ClassName("io.github.fruit.annotations", "Pick").copy(nullable = true))
                            .returns(classDeclaration.toClassName().copy(nullable = true))
                            .addCode(generateReadLogic(classDeclaration))
                            .build()
                    )
                    .build()
            )
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(false, classDeclaration.containingFile!!))
    }

    private fun generateReadLogic(classDeclaration: KSClassDeclaration): CodeBlock {
        val builder = CodeBlock.builder()
        val className = classDeclaration.toClassName()
        
        builder.addStatement("var currentElement = element")
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
        return builder.build()
    }

    private fun generateReadForType(builder: CodeBlock.Builder, type: KSType, css: String, attr: String, ownText: Boolean) {
        val qualifiedName = type.declaration.qualifiedName?.asString()
        val pickExpr = "Pick(value = %S, attr = %S, ownText = %L)"

        when (qualifiedName) {
            "kotlin.String" -> builder.add("io.github.fruit.bind.BasicPickAdapters.STRING_ADAPTER.read(currentElement, $pickExpr) ?: \"\"", css, attr, ownText)
            "kotlin.Int" -> builder.add("io.github.fruit.bind.BasicPickAdapters.INT_ADAPTER.read(currentElement, $pickExpr) ?: 0", css, attr, ownText)
            "kotlin.Long" -> builder.add("io.github.fruit.bind.BasicPickAdapters.LONG_ADAPTER.read(currentElement, $pickExpr) ?: 0L", css, attr, ownText)
            "kotlin.Float" -> builder.add("io.github.fruit.bind.BasicPickAdapters.FLOAT_ADAPTER.read(currentElement, $pickExpr) ?: 0.0f", css, attr, ownText)
            "kotlin.Boolean" -> builder.add("io.github.fruit.bind.BasicPickAdapters.BOOLEAN_ADAPTER.read(currentElement, $pickExpr) ?: false", css, attr, ownText)
            "kotlin.collections.List" -> {
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
                val nestedClassName = type.declaration.simpleName.asString()
                val adapterName = "${nestedClassName}PickAdapter"
                builder.add("%T().read(currentElement.selectFirst(%S) ?: currentElement, null)!!", 
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
                        processedClasses.forEach { clazz ->
                            val packageName = clazz.packageName.asString()
                            val className = clazz.simpleName.asString()
                            val adapterClassName = ClassName(packageName, "${className}PickAdapter")
                            addStatement("registerAdapter(%T::class, %T())", clazz.toClassName(), adapterClassName)
                        }
                    }.build())
                    .build()
            )
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(false, *processedClasses.mapNotNull { it.containingFile }.toTypedArray()))
    }
}

class FruitProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FruitProcessor(environment.codeGenerator, environment.logger)
    }
}
