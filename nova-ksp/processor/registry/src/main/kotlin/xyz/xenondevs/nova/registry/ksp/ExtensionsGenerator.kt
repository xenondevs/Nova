package xyz.xenondevs.nova.registry.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal class ExtensionsGenerator(private val codeGenerator: CodeGenerator) {
    
    private val registryPackage = "xyz.xenondevs.nova.registry"
    private val paperRegistryPackage = "io.papermc.paper.registry"
    private val registryEntryClass = ClassName(registryPackage, "RegistryEntry")
    private val registryEntryPaperClass = registryEntryClass.nestedClass("Paper")
    private val typedKeyClass = ClassName(paperRegistryPackage, "TypedKey")
    private val registryKeyClass = ClassName(paperRegistryPackage, "RegistryKey")
    private val suppressUnusedAnnotation = AnnotationSpec.builder(Suppress::class)
        .addMember("%S", "unused")
        .build()
    
    fun generateTypedKeyExtensions(registryKey: KSClassDeclaration) {
        val fileSpec = FileSpec.builder(registryPackage, "TypedKeyExtensions")
            .addAnnotation(suppressUnusedAnnotation)
        
        registryKey.nonDeprecatedRegistryKeyProperties()
            .forEach { property ->
                val keyName = property.simpleName.asString()
                val keyType = property.primaryTypeArgument()
                val keyTypeName = keyType.toTypeName()
                val keyTypeDeclaration = keyType.declaration as KSClassDeclaration
                fileSpec.addProperty(
                    PropertySpec.builder("typedKey", typedKeyClass.parameterizedBy(keyTypeName))
                        .receiver(keyTypeName)
                        .addKdoc(
                            "Gets the typed key for this [%T] in [%T.%L].",
                            keyTypeDeclaration.toClassName(),
                            registryKeyClass,
                            keyName
                        )
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement("return %T.create(%T.%L, key)", typedKeyClass, registryKeyClass, keyName)
                                .build()
                        )
                        .build()
                )
            }
        
        fileSpec.build().writeTo(codeGenerator, Dependencies(false, registryKey.containingFile!!))
    }
    
    fun generateEntryExtensions(registryKey: KSClassDeclaration) {
        val fileSpec = FileSpec.builder(registryPackage, "EntryExtensions")
            .addAnnotation(suppressUnusedAnnotation)
        
        registryKey.nonDeprecatedRegistryKeyProperties()
            .forEach { property ->
                val keyName = property.simpleName.asString()
                val keyType = property.primaryTypeArgument()
                val keyTypeName = keyType.toTypeName()
                val keyTypeDeclaration = keyType.declaration as KSClassDeclaration
                fileSpec.addProperty(
                    PropertySpec.builder("entry", registryEntryPaperClass.parameterizedBy(keyTypeName))
                        .receiver(keyTypeName)
                        .addKdoc(
                            "Gets the registry entry for this [%T] in [%T.%L].",
                            keyTypeDeclaration.toClassName(),
                            registryKeyClass,
                            keyName
                        )
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement("return %T.paper(%T.%L, this)", registryEntryClass, registryKeyClass, keyName)
                                .build()
                        )
                        .build()
                )
            }
        
        fileSpec.build().writeTo(codeGenerator, Dependencies(false, registryKey.containingFile!!))
    }
}
