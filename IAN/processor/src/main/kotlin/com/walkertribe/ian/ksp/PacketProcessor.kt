package com.walkertribe.ian.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.walkertribe.ian.protocol.PacketSubtype
import com.walkertribe.ian.protocol.PacketType
import com.walkertribe.ian.util.JamCrc
import com.walkertribe.ian.util.Util.toHex
import org.koin.core.annotation.Single

class PacketProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    private val visitor: PacketVisitor by lazy { PacketVisitor(codeGenerator) }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val packetTypeClassName = PacketType::class.qualifiedName ?: return emptyList()
        val packetSubtypeClassName = PacketSubtype::class.qualifiedName ?: return emptyList()

        val symbols = resolver.getSymbolsWithAnnotation(packetTypeClassName)
            .filterIsInstance<KSClassDeclaration>()
            .associateBy {
                JamCrc.compute(it.getAnnotationsByType(PacketType::class).first().type)
            }.toSortedMap()

        val subtypeSymbols = resolver.getSymbolsWithAnnotation(packetSubtypeClassName)
            .filterIsInstance<KSClassDeclaration>()
            .associateBy {
                it.getAnnotationsByType(PacketSubtype::class).first().subtype
            }.toSortedMap()

        val packetClasses = (symbols.values + subtypeSymbols.values).filter { !it.isAbstract() }
        if (packetClasses.isEmpty()) return emptyList()

        packetClasses.forEach { it.accept(visitor, Unit) }

        val abstractSymbols =
            symbols.values.filter { it.isAbstract() }.map { it.simpleName.asString() }.toSortedSet()
        val subtypeMap = subtypeSymbols.values.groupBy { cls ->
            cls.superTypes.map { superType ->
                superType.resolve().declaration.simpleName.asString()
            }.first(abstractSymbols::contains)
        }

        val factoryClassName = ClassName(MAIN_PACKAGE, "PacketFactory")
        val factoryClassWildcardName = factoryClassName.plusParameter(STAR)

        val fileSpecBuilder = FileSpec.builder(MAIN_PACKAGE, FILENAME)
        packetClasses.filterNot { it.packageName.asString() == MAIN_PACKAGE }.forEach {
            fileSpecBuilder.addImport(it.packageName.asString(), makeFactoryClassName(it))
        }

        val getFactoryFunBuilder = FunSpec.builder("getFactory")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("type", Int::class)
            .addParameter("subtype", Byte::class)
            .returns(factoryClassWildcardName.copy(nullable = true))
            .beginControlFlow("return when (type)")

        abstractSymbols.forEach { cls ->
            getFactoryFunBuilder.addStatement(
                "%1L -> %1L_FACTORY_MAP[subtype]",
                makeConstantName(cls),
            )
        }

        getFactoryFunBuilder.addStatement("else -> FACTORY_MAP[type]")
            .endControlFlow()

        val factoryMapBuilder = PropertySpec.builder(
            "FACTORY_MAP",
            Map::class.asTypeName().parameterizedBy(
                Int::class.asTypeName(),
                factoryClassWildcardName,
            ),
            KModifier.PRIVATE,
        ).initializer(
            symbols.values.filter {
                !it.isAbstract()
            }.joinToString(prefix = "mapOf(", postfix = "\n)") {
                val factoryName = makeFactoryClassName(it)
                "\n${makeConstantName(it)} to $factoryName"
            }
        )

        val protocolCompanionBuilder = TypeSpec.companionObjectBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addProperties(
                symbols.map { (hash, symbol) ->
                    PropertySpec.builder(
                        makeConstantName(symbol),
                        Int::class,
                        KModifier.PRIVATE,
                        KModifier.CONST,
                    ).initializer("%L", hash).build()
                }
            )
            .addProperty(factoryMapBuilder.build())

        subtypeMap.forEach { (parentName, subtypeClasses) ->
            val typeName = makeConstantName(parentName) + "_"

            val subtypeClassMap = subtypeClasses.associateBy {
                it.getAnnotationsByType(PacketSubtype::class).first().subtype
            }.toSortedMap()

            val subtypeFactoryMapBuilder = PropertySpec.builder(
                "${typeName}FACTORY_MAP",
                Map::class.asTypeName().parameterizedBy(
                    Byte::class.asTypeName(),
                    factoryClassWildcardName,
                ),
                KModifier.PRIVATE,
            ).initializer(
                subtypeClasses.joinToString(prefix = "mapOf(", postfix = "\n)") {
                    "\n$typeName${makeConstantName(it)} to ${makeFactoryClassName(it)}"
                }
            )

            protocolCompanionBuilder.addProperties(
                subtypeClassMap.map { (subtype, symbol) ->
                    PropertySpec.builder(
                        "$typeName${makeConstantName(symbol)}",
                        Byte::class,
                        KModifier.PRIVATE,
                        KModifier.CONST,
                    ).initializer("0x%L", subtype.toHex()).build()
                }
            ).addProperty(subtypeFactoryMapBuilder.build())
        }

        val protocolClassBuilder = TypeSpec.classBuilder(FILENAME)
            .addSuperinterface(ClassName(MAIN_PACKAGE, "Protocol"))
            .addAnnotation(Single::class)
            .addFunction(getFactoryFunBuilder.build())
            .addType(protocolCompanionBuilder.build())

        fileSpecBuilder.addType(protocolClassBuilder.build())

        fileSpecBuilder.build().writeTo(
            codeGenerator = codeGenerator,
            aggregating = false,
        )

        return emptyList()
    }

    internal companion object {
        const val MAIN_PACKAGE = "com.walkertribe.ian.protocol"
        const val FILENAME = "KoinProtocol"
        private const val PACKET_LENGTH = 6
        private const val FACTORY_LENGTH = 7

        private val factoryNames = mutableMapOf<String, String>()
        private val constantNames = mutableMapOf<String, String>()

        fun makeFactoryClassName(classDeclaration: KSClassDeclaration): String {
            val className = classDeclaration.simpleName.asString()
            val factoryName = factoryNames[className]
            if (factoryName != null) return factoryName

            val parentName = classDeclaration.parentDeclaration?.run {
                simpleName.asString()
            }
            val newFactoryName = "${className}${parentName ?: ""}Factory"
            factoryNames[className] = newFactoryName
            return newFactoryName
        }

        fun makeConstantName(classDeclaration: KSClassDeclaration): String =
            makeConstantName(makeFactoryClassName(classDeclaration).dropLast(FACTORY_LENGTH))

        fun makeConstantName(name: String): String {
            val className = name.dropLast(PACKET_LENGTH)
            val constantName = constantNames[className]
            if (constantName != null) return constantName

            val uppercase = className.indices.filter { className[it].isUpperCase() }.drop(1).toSet()
            val newConstantName = className.toCharArray().mapIndexed { index, c ->
                if (uppercase.contains(index)) "_$c" else "$c".uppercase()
            }.joinToString("")
            constantNames[className] = newConstantName
            return newConstantName
        }
    }
}
