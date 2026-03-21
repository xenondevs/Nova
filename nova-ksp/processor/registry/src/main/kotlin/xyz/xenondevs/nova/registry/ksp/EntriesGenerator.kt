package xyz.xenondevs.nova.registry.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal class EntriesGenerator(private val codeGenerator: CodeGenerator) {
    
    private val registryPackage = "xyz.xenondevs.nova.registry"
    private val entriesPackage = "$registryPackage.entries"
    private val registryEntryClass = ClassName(registryPackage, "RegistryEntry")
    private val registryEntryPaperClass = registryEntryClass.nestedClass("Paper")
    private val registryEntrySetPaperTagClass = ClassName(registryPackage, "RegistryEntrySet")
        .nestedClass("Paper")
        .nestedClass("Tag")
    private val registryEntrySetOfMember = MemberName(registryPackage, "registryEntrySetOf")
    private val jvmFieldAnnotation = AnnotationSpec.builder(JvmField::class).build()
    private val suppressUnusedAnnotation = AnnotationSpec.builder(Suppress::class)
        .addMember("%S", "unused")
        .build()
    
    fun generateEntriesFile(keys: KSClassDeclaration) {
        val keysClassName = keys.toClassName()
        val prettyName = keys.simpleName.getShortName().removeSuffix("Keys")
        val objectSpec = TypeSpec.objectBuilder("${prettyName}Entries")
            .addKdoc("Contains corresponding entries for keys in [%T].", keysClassName)
        
        keys.publicStaticPropertiesOfType("TypedKey")
            .forEach { property ->
                val propertyName = property.simpleName.getShortName()
                val valueType = property.primaryTypeArgument().toTypeName()
                val returnType = registryEntryPaperClass.parameterizedBy(valueType)
                objectSpec.addProperty(
                    PropertySpec.builder(propertyName, returnType)
                        .addAnnotation(jvmFieldAnnotation)
                        .addKdoc("An entry for [%T.%L].", keysClassName, propertyName)
                        .initializer("%T.paper(%T.%L)", registryEntryClass, keysClassName, propertyName)
                        .build()
                )
            }
        
        FileSpec.builder(entriesPackage, "${prettyName}Entries")
            .addAnnotation(suppressUnusedAnnotation)
            .addType(objectSpec.build())
            .build()
            .writeTo(codeGenerator, Dependencies(false, keys.containingFile!!))
    }
    
    fun generateEntrySetsFile(tagKeys: KSClassDeclaration) {
        val tagKeysClassName = tagKeys.toClassName()
        val prettyName = tagKeys.simpleName.getShortName().removeSuffix("TagKeys")
        val objectSpec = TypeSpec.objectBuilder("${prettyName}Tags")
            .addKdoc("Contains corresponding entry sets for tags in [%T].", tagKeysClassName)
        
        tagKeys.publicStaticPropertiesOfType("TagKey")
            .forEach { property ->
                val propertyName = property.simpleName.getShortName()
                val valueType = property.primaryTypeArgument().toTypeName()
                val returnType = registryEntrySetPaperTagClass.parameterizedBy(valueType)
                objectSpec.addProperty(
                    PropertySpec.builder(propertyName, returnType)
                        .addAnnotation(jvmFieldAnnotation)
                        .addKdoc("An entry set containing the tag [%T.%L].", tagKeysClassName, propertyName)
                        .initializer("%M(%T.%L)", registryEntrySetOfMember, tagKeysClassName, propertyName)
                        .build()
                )
            }
        
        FileSpec.builder(entriesPackage, "${prettyName}Tags")
            .addAnnotation(suppressUnusedAnnotation)
            .addType(objectSpec.build())
            .build()
            .writeTo(codeGenerator, Dependencies(false, tagKeys.containingFile!!))
    }
}
