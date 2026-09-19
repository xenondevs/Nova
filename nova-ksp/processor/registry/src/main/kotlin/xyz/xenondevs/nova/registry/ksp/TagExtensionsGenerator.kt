package xyz.xenondevs.nova.registry.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal class TagExtensionsGenerator(private val codeGenerator: CodeGenerator) {
    
    private val registryPackage = "xyz.xenondevs.nova.registry"
    private val registryEntryPaperClass = ClassName(registryPackage, "RegistryEntry")
        .nestedClass("Paper")
    private val registryEntrySetPaperTagClass = ClassName(registryPackage, "RegistryEntrySet")
        .nestedClass("Paper")
        .nestedClass("Tag")
    private val keyClass = ClassName("net.kyori.adventure.key", "Key")
    private val providerClass = ClassName("xyz.xenondevs.commons.provider", "Provider")
    private val providerLookupClass = ClassName(registryPackage, "ProviderLookup")
    private val keyToTagLookupMember = MemberName(registryPackage, "keyToTagLookup")
    private val suppressAnnotation = AnnotationSpec.builder(Suppress::class)
        .addMember("%S, %S", "unused", "DEPRECATION")
        .build()
    
    fun generateTagExtensions(tagKeysDeclarations: List<KSClassDeclaration>) {
        val fileSpec = FileSpec.builder(registryPackage, "TagExtensions")
            .addAnnotation(suppressAnnotation)
        
        for (tagKeys in tagKeysDeclarations) {
            val firstTagProperty = tagKeys.publicStaticPropertiesOfType("TagKey").firstOrNull()
                ?: continue
            val tagKeysClassName = tagKeys.toClassName()
            val valueType = firstTagProperty.primaryTypeArgument().toTypeName()
            val valueTypeDeclaration = firstTagProperty.primaryTypeArgument().declaration as KSClassDeclaration
            val valueClassName = valueTypeDeclaration.toClassName()
            val valueTypeName = valueClassName.simpleNames.joinToString("")
            val lookupNamePrefix = valueTypeName.replaceFirstChar(Char::lowercaseChar)
            val tagType = registryEntrySetPaperTagClass.parameterizedBy(valueType)
            val tagSetType = SET.parameterizedBy(tagType)
            val tagsProviderType = providerClass.parameterizedBy(tagSetType)
            val registryEntryType = registryEntryPaperClass.parameterizedBy(valueType)
            
            val elementLookup = PropertySpec.builder(
                "${lookupNamePrefix}TagLookup",
                providerLookupClass.parameterizedBy(keyClass, tagSetType),
                KModifier.PRIVATE
            ).initializer(
                "%M(%T.%L.registryKey())",
                keyToTagLookupMember,
                tagKeysClassName,
                firstTagProperty.simpleName.getShortName()
            ).build()
            fileSpec.addProperty(elementLookup)
            
            fileSpec.addProperty(
                PropertySpec.builder("tags", tagsProviderType)
                    .receiver(valueType)
                    .addKdoc("Gets the tags that contain this [%T].", valueClassName)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return %N[this.key()]", elementLookup)
                            .build()
                    )
                    .build()
            )
            
            fileSpec.addProperty(
                PropertySpec.builder("tags", tagsProviderType)
                    .receiver(registryEntryType)
                    .addAnnotation(
                        AnnotationSpec.builder(JvmName::class)
                            .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                            .addMember("%S", "get${valueTypeName}EntryTags")
                            .build()
                    )
                    .addKdoc("Gets the tags that contain this [%T] entry.", valueClassName)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return %N[this.key.key()]", elementLookup)
                            .build()
                    )
                    .build()
            )
        }
        
        val sourceFiles = tagKeysDeclarations.mapNotNull { it.containingFile }.toTypedArray()
        fileSpec.build().writeTo(codeGenerator, Dependencies(true, *sourceFiles))
    }
    
}
