package xyz.xenondevs.nova.registry.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.ksp.writeTo

internal class RegistryKeysGenerator(private val codeGenerator: CodeGenerator) {
    
    private val registryPackage = "xyz.xenondevs.nova.registry"
    private val registryKeyClass = ClassName("io.papermc.paper.registry", "RegistryKey")
    private val suppressUnusedAnnotation = AnnotationSpec.builder(Suppress::class)
        .addMember("%S", "unused")
        .build()
    
    fun generateAllRegistryKeys(registryKey: KSClassDeclaration) {
        val properties = registryKey.registryKeyProperties().toList()
        
        val setType = SET.parameterizedBy(registryKeyClass.parameterizedBy(STAR))
        
        val initializer = CodeBlock.builder()
            .add("setOf(\n")
            .indent()
        for ((index, property) in properties.withIndex()) {
            val separator = if (index < properties.size - 1) "," else ""
            initializer.add("%T.%L$separator\n", registryKeyClass, property.simpleName.asString())
        }
        initializer.unindent()
            .add(")")
        
        val propertySpec = PropertySpec.builder("ALL_REGISTRY_KEYS", setType)
            .addKdoc("A set of all [%T]s.", registryKeyClass)
            .initializer(initializer.build())
            .build()
        
        val fileSpec = FileSpec.builder(registryPackage, "AllRegistryKeys")
            .addAnnotation(suppressUnusedAnnotation)
            .addProperty(propertySpec)
            .build()
        
        fileSpec.writeTo(codeGenerator, Dependencies(false, registryKey.containingFile!!))
    }
}
