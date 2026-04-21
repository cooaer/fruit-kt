package io.github.fruit.ksp

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
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
                if (generateAdapter(clazz)) {
                    classesToRegister.add(clazz)
                }
                processedQualifiedNames.add(qualifiedName)
            }
        }

        return emptyList()
    }

    override fun finish() {
        if (classesToRegister.isEmpty()) return
        generateRegistry()
    }

    private fun generateAdapter(classDeclaration: KSClassDeclaration): Boolean {
        validateClass(classDeclaration)?.let { error ->
            logger.error(error, classDeclaration)
            return false
        }

        val packageName = classDeclaration.packageName.asString()
        val fullName = getFullSimpleName(classDeclaration)
        val adapterName = "${fullName}PickAdapter"

        val fileSpec = FileSpec.builder(packageName, adapterName)
            .addImport("io.github.fruit.annotations", "Attrs")
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
        return true
    }

    private fun validateClass(classDeclaration: KSClassDeclaration): String? {
        if (inheritsCollection(classDeclaration)) {
            return "@Pulp type ${classDeclaration.simpleName.asString()} must use an explicit List property instead of inheriting from Collection/MutableList."
        }

        val primaryConstructor = classDeclaration.primaryConstructor
            ?: return "@Pulp type ${classDeclaration.simpleName.asString()} must declare a primary constructor."
        val constructorParameters = primaryConstructor.parameters.associateBy { it.name?.asString() }
        val rawResponseSupported = implementsRawResponseHolder(classDeclaration)

        classDeclaration.getDeclaredProperties().forEach { property ->
            val pickAnnotation = property.pickAnnotation()
            val name = property.simpleName.asString()
            val constructorParameter = constructorParameters[name]

            if (pickAnnotation != null) {
                if (constructorParameter == null) {
                    return "@Pick property $name in ${classDeclaration.simpleName.asString()} must be declared in the primary constructor."
                }
                if (property.isMutable) {
                    return "@Pick property $name in ${classDeclaration.simpleName.asString()} must be immutable."
                }
            }

            if (constructorParameter != null && property.isMutable) {
                return "Primary constructor property $name in ${classDeclaration.simpleName.asString()} must be immutable."
            }
        }

        primaryConstructor.parameters.forEach { parameter ->
            val name = parameter.name?.asString() ?: return@forEach
            val property = classDeclaration.getDeclaredProperties()
                .firstOrNull { it.simpleName.asString() == name }
                ?: return@forEach
            val isRawResponse = rawResponseSupported && name == RAW_RESPONSE_PROPERTY
            val hasPick = property.pickAnnotation() != null
            if (!hasPick && !isRawResponse && !parameter.hasDefault) {
                return "Primary constructor parameter $name in ${classDeclaration.simpleName.asString()} must either be annotated with @Pick, be rawResponse, or declare a default value."
            }
        }

        return null
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
        val classPulp = classDeclaration.pulpAnnotation()
        val cssValue = classPulp?.arguments
            ?.find { it.name?.asString() == "value" }
            ?.value as? String ?: ""
        val rawResponseSupported = implementsRawResponseHolder(classDeclaration)
        val primaryConstructor = requireNotNull(classDeclaration.primaryConstructor)
        val propertiesByName = classDeclaration.getDeclaredProperties().associateBy { it.simpleName.asString() }

        builder.addStatement("var currentElement = element")
        if (cssValue.isNotEmpty()) {
            builder.addStatement("currentElement = element.selectFirst(%S) ?: return null", cssValue)
        }

        builder.add("return %T(\n", className)
        builder.indent()

        val emittedNames = mutableListOf<String>()
        primaryConstructor.parameters.forEach { parameter ->
            val name = parameter.name?.asString() ?: return@forEach
            val property = propertiesByName[name] ?: return@forEach
            val pickAnnotation = property.pickAnnotation()
            val isRawResponse = rawResponseSupported && name == RAW_RESPONSE_PROPERTY

            when {
                isRawResponse -> {
                    emittedNames += name
                    builder.add("%L = currentElement.outerHtml(),\n", name)
                }

                pickAnnotation != null -> {
                    emittedNames += name
                    builder.add("%L = ", name)
                    generateReadForType(
                        builder = builder,
                        type = property.type.resolve(),
                        pickAnnotation = pickAnnotation,
                        elementName = "currentElement"
                    )
                    builder.add(",\n")
                }
            }
        }

        if (emittedNames.isEmpty()) {
            builder.add("\n")
        }

        builder.unindent()
        builder.add(")\n")
        return builder.build()
    }

    private fun generateReadForType(
        builder: CodeBlock.Builder,
        type: KSType,
        pickAnnotation: KSAnnotation,
        elementName: String
    ) {
        val args = pickAnnotation.arguments
        val css = args.find { it.name?.asString() == "value" }?.value as? String ?: ""
        val attr = args.find { it.name?.asString() == "attr" }?.value as? String ?: "text"
        val ownText = args.find { it.name?.asString() == "ownText" }?.value as? Boolean ?: false
        val qualifiedName = type.declaration.qualifiedName?.asString().orEmpty()

        when (qualifiedName) {
            "kotlin.String" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.STRING_ADAPTER.read(%L, %S, %S, %L) ?: \"\"",
                elementName,
                css,
                attr,
                ownText
            )

            "kotlin.Int" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.INT_ADAPTER.read(%L, %S, %S, %L) ?: 0",
                elementName,
                css,
                attr,
                ownText
            )

            "kotlin.Long" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.LONG_ADAPTER.read(%L, %S, %S, %L) ?: 0L",
                elementName,
                css,
                attr,
                ownText
            )

            "kotlin.Float" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.FLOAT_ADAPTER.read(%L, %S, %S, %L) ?: 0.0f",
                elementName,
                css,
                attr,
                ownText
            )

            "kotlin.Boolean" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.BOOLEAN_ADAPTER.read(%L, %S, %S, %L) ?: false",
                elementName,
                css,
                attr,
                ownText
            )

            "kotlin.collections.List", "kotlin.collections.MutableList", "java.util.List" -> {
                val elementType = type.arguments.firstOrNull()?.type?.resolve()
                if (elementType == null) {
                    builder.add("emptyList()")
                    return
                }
                builder.add("%L.select(%S).map { ", elementName, css)
                generateCollectionElementRead(builder, elementType, attr, ownText, "it")
                builder.add(" }")
            }

            else -> {
                val nestedClass = type.declaration as? KSClassDeclaration
                val nestedFullName =
                    if (nestedClass != null) getFullSimpleName(nestedClass) else type.declaration.simpleName.asString()
                val adapterName = "${nestedFullName}PickAdapter"
                if (css.isNotEmpty()) {
                    builder.add(
                        "%T().read(%L.selectFirst(%S) ?: %L, \"\", \"text\", false)",
                        ClassName(type.declaration.packageName.asString(), adapterName),
                        elementName,
                        css,
                        elementName
                    )
                } else {
                    builder.add(
                        "%T().read(%L, \"\", \"text\", false)",
                        ClassName(type.declaration.packageName.asString(), adapterName),
                        elementName
                    )
                }

                if (!type.isMarkedNullable) {
                    builder.add("!!")
                }
            }
        }
    }

    private fun generateCollectionElementRead(
        builder: CodeBlock.Builder,
        type: KSType,
        attr: String,
        ownText: Boolean,
        elementName: String
    ) {
        val qualifiedName = type.declaration.qualifiedName?.asString().orEmpty()
        when (qualifiedName) {
            "kotlin.String" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.STRING_ADAPTER.read(%L, \"\", %S, %L) ?: \"\"",
                elementName,
                attr,
                ownText
            )

            "kotlin.Int" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.INT_ADAPTER.read(%L, \"\", %S, %L) ?: 0",
                elementName,
                attr,
                ownText
            )

            "kotlin.Long" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.LONG_ADAPTER.read(%L, \"\", %S, %L) ?: 0L",
                elementName,
                attr,
                ownText
            )

            "kotlin.Float" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.FLOAT_ADAPTER.read(%L, \"\", %S, %L) ?: 0.0f",
                elementName,
                attr,
                ownText
            )

            "kotlin.Boolean" -> builder.add(
                "io.github.fruit.bind.BasicPickAdapters.BOOLEAN_ADAPTER.read(%L, \"\", %S, %L) ?: false",
                elementName,
                attr,
                ownText
            )

            else -> {
                val nestedClass = type.declaration as? KSClassDeclaration
                val nestedFullName =
                    if (nestedClass != null) getFullSimpleName(nestedClass) else type.declaration.simpleName.asString()
                val adapterName = "${nestedFullName}PickAdapter"
                builder.add(
                    "%T().read(%L, \"\", \"text\", false)%L",
                    ClassName(type.declaration.packageName.asString(), adapterName),
                    elementName,
                    if (type.isMarkedNullable) "" else "!!"
                )
            }
        }
    }

    private fun inheritsCollection(classDeclaration: KSClassDeclaration): Boolean {
        val collectionNames = setOf(
            "kotlin.collections.List",
            "kotlin.collections.MutableList",
            "java.util.List",
            "kotlin.collections.Collection",
            "java.util.Collection",
            "kotlin.collections.ArrayList",
            "java.util.ArrayList"
        )

        return classDeclaration.superTypes.any { superType ->
            val resolved = superType.resolve()
            val name = resolved.declaration.qualifiedName?.asString()
            if (name in collectionNames) {
                true
            } else {
                (resolved.declaration as? KSClassDeclaration)?.let(::inheritsCollection) == true
            }
        }
    }

    private fun implementsRawResponseHolder(classDeclaration: KSClassDeclaration): Boolean {
        return classDeclaration.getAllSuperTypes().any {
            it.declaration.qualifiedName?.asString() == "io.github.fruit.RawResponseHolder"
        }
    }

    private fun generateRegistry() {
        val fileSpec = FileSpec.builder("io.github.fruit", "FruitRegistry")
            .addFunction(
                FunSpec.builder("registerGeneratedAdapters")
                    .receiver(ClassName("io.github.fruit", "Fruit"))
                    .addCode(
                        CodeBlock.builder().apply {
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
                        }.build()
                    )
                    .build()
            )
            .build()

        fileSpec.writeTo(
            codeGenerator,
            Dependencies(false, *classesToRegister.mapNotNull { it.containingFile }.toTypedArray())
        )
    }

    private fun KSClassDeclaration.pulpAnnotation() = annotations.find {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.github.fruit.annotations.Pulp"
    }

    private fun KSPropertyDeclaration.pickAnnotation() = annotations.find {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.github.fruit.annotations.Pick"
    }

    private companion object {
        const val RAW_RESPONSE_PROPERTY = "rawResponse"
    }
}

class FruitProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return FruitProcessor(environment.codeGenerator, environment.logger)
    }
}
