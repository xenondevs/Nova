package xyz.xenondevs.nova.registry.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal class TypeAliasesGenerator(private val codeGenerator: CodeGenerator) {
    
    private val registryPackage = "xyz.xenondevs.nova.registry"
    private val serializationPackage = "xyz.xenondevs.nova.serialization.kotlinx"
    private val aliasPackage = "xyz.xenondevs.nova.registry.alias"
    private val serializableClass = ClassName("kotlinx.serialization", "Serializable")
    private val registryEntryPaperClass = ClassName(registryPackage, "RegistryEntry", "Paper")
    private val registryEntrySetPaperClass = ClassName(registryPackage, "RegistryEntrySet", "Paper")
    private val registryKeySetClass = ClassName("io.papermc.paper.registry.set", "RegistryKeySet")
    private val tagKeyClass = ClassName("io.papermc.paper.registry.tag", "TagKey")
    private val typedKeyClass = ClassName("io.papermc.paper.registry", "TypedKey")
    private val suppressUnusedAnnotation = AnnotationSpec.builder(Suppress::class)
        .addMember("%S", "unused")
        .build()
    
    /**
     * Maps serializer suffix to a function that produces the aliased type given the element type.
     */
    private val typeAliasEntries: List<Pair<String, (TypeName) -> TypeName>> = listOf(
        // does not generate type aliases for the base type for now
        // in the future, this may make sense to do with e.g. a detekt compiler plugin that could nudge
        // users towards using the serializable type alias immediately while writing code
        "Entry" to { elementType -> registryEntryPaperClass.parameterizedBy(elementType) },
        "EntrySet" to { elementType -> registryEntrySetPaperClass.parameterizedBy(elementType) },
        "KeySet" to { elementType -> registryKeySetClass.parameterizedBy(elementType) },
        "TagKey" to { elementType -> tagKeyClass.parameterizedBy(elementType) },
        "Key" to { elementType -> typedKeyClass.parameterizedBy(elementType) },
    )
    
    fun generateSerializableTypeAliases(registryKey: KSClassDeclaration) {
        val fileSpec = FileSpec.builder(aliasPackage, "TypeAliases")
            .addAnnotation(suppressUnusedAnnotation)
        
        registryKey.registryKeyProperties()
            .forEach { property ->
                val keyType = property.primaryTypeArgument()
                val keyTypeName = keyType.toTypeName()
                val keyTypeDeclaration = keyType.declaration as KSClassDeclaration
                val typeName = keyTypeDeclaration.toClassName().simpleNames.joinToString("")
                
                for ((suffix, typeProvider) in typeAliasEntries) {
                    val serializerClassName = ClassName(serializationPackage, "${typeName}${suffix}Serializer")
                    val serializableAnnotation = AnnotationSpec.builder(serializableClass)
                        .addMember("%T::class", serializerClassName)
                        .build()
                    val aliasedRawType = typeProvider(keyTypeName)
                    val aliasedType = aliasedRawType.copy(annotations = listOf(serializableAnnotation))
                    
                    fileSpec.addTypeAlias(
                        TypeAliasSpec.builder("${typeName}${suffix}", aliasedType)
                            .addKdoc("Serializable type alias for `%T` using [%T].", aliasedRawType, serializerClassName)
                            .build()
                    )
                }
            }
        
        fileSpec.build().writeTo(codeGenerator, Dependencies(false, registryKey.containingFile!!))
    }
}
