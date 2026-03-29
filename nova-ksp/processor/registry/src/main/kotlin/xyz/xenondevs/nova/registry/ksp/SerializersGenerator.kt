package xyz.xenondevs.nova.registry.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal class SerializersGenerator(private val codeGenerator: CodeGenerator) {
    
    private val serializationPackage = "xyz.xenondevs.nova.serialization.kotlinx"
    private val paperRegistryPackage = "io.papermc.paper.registry"
    private val registryKeyClass = ClassName(paperRegistryPackage, "RegistryKey")
    private val registryAccessClass = ClassName(paperRegistryPackage, "RegistryAccess")
    private val suppressUnusedAnnotation = AnnotationSpec.builder(Suppress::class)
        .addMember("%S", "unused")
        .build()
    
    private val serializerSuperclasses = listOf(
        "" to ClassName(serializationPackage, "PaperRegistryElementSerializer"),
        "Entry" to ClassName(serializationPackage, "PaperRegistryEntrySerializer"),
        "EntrySet" to ClassName(serializationPackage, "PaperRegistryEntrySetSerializer"),
        "KeySet" to ClassName(serializationPackage, "RegistryKeySetSerializer"),
        "TagKey" to ClassName(serializationPackage, "TagKeySerializer"),
        "Key" to ClassName(serializationPackage, "TypedKeySerializer"),
    )
    
    private val serializersModuleBuilderClass = ClassName("kotlinx.serialization.modules", "SerializersModuleBuilder")
    private val contextualMember = MemberName("kotlinx.serialization.modules", "contextual")
    
    private val paperElementSerializerClass = ClassName(serializationPackage, "PaperRegistryElementSerializer")
    
    private data class RegistryTypeInfo(
        val keyName: String,
        val keyTypeName: TypeName,
        val keyTypeClassName: ClassName,
        val typeName: String,
    )
    
    fun generatePaperRegistrySerializers(registryKey: KSClassDeclaration) {
        val fileSpec = FileSpec.builder(serializationPackage, "PaperRegistrySerializers")
            .addAnnotation(suppressUnusedAnnotation)
        
        val typeInfos = registryKey.registryKeyProperties()
            .map { property ->
                val keyType = property.primaryTypeArgument()
                val keyTypeDeclaration = keyType.declaration as KSClassDeclaration
                RegistryTypeInfo(
                    keyName = property.simpleName.asString(),
                    keyTypeName = keyType.toTypeName(),
                    keyTypeClassName = keyTypeDeclaration.toClassName(),
                    typeName = keyTypeDeclaration.toClassName().simpleNames.joinToString(""),
                )
            }
            .toList()
        
        // Generate serializer objects
        for (info in typeInfos) {
            for ((suffix, superclass) in serializerSuperclasses) {
                fileSpec.addType(
                    TypeSpec.objectBuilder("${info.typeName}${suffix}Serializer")
                        .addKdoc(
                            "A [%T] for [%T] in [%T.%L] using the default [RegistryAccess][%T.registryAccess].",
                            superclass,
                            info.keyTypeClassName,
                            registryKeyClass,
                            info.keyName,
                            registryAccessClass
                        )
                        .superclass(superclass.parameterizedBy(info.keyTypeName))
                        .addSuperclassConstructorParameter("%T.%L", registryKeyClass, info.keyName)
                        .build()
                )
            }
        }
        
        // Generate registerPaperRegistryEntrySerializers() function
        fileSpec.addFunction(generateRegisterFunction(typeInfos))
        
        fileSpec.build().writeTo(codeGenerator, Dependencies(false, registryKey.containingFile!!))
    }
    
    private fun generateRegisterFunction(typeInfos: List<RegistryTypeInfo>): FunSpec {
        val body = CodeBlock.builder()
        
        // contextual(ItemType::class, PaperRegistryElementSerializer(RegistryKey.ITEM, registryAccess))
        for (info in typeInfos) {
            body.addStatement(
                "%M(%T::class, %T(%T.%L, registryAccess))",
                contextualMember,
                info.keyTypeClassName,
                paperElementSerializerClass,
                registryKeyClass,
                info.keyName
            )
        }
        
        return FunSpec.builder("contextualPaperRegistryElementSerializers")
            .addKdoc("Registers contextual element serializers for all Paper registries.")
            .receiver(serializersModuleBuilderClass)
            .addParameter(
                ParameterSpec.builder("registryAccess", registryAccessClass)
                    .defaultValue("%T.registryAccess()", registryAccessClass)
                    .build()
            )
            .addCode(body.build())
            .build()
    }
}
