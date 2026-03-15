@file:OptIn(KspExperimental::class)

package xyz.xenondevs.nova.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import xyz.xenondevs.nova.ksp.annotation.GenerateFlatMapExtensions

class FlatMapExtensionProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    
    private lateinit var providerType: KSType
    
    private var invoked = false
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked)
            return emptyList()
        invoked = true
        
        providerType = resolver.getClassDeclarationByName("xyz.xenondevs.commons.provider.Provider")!!.asStarProjectedType()
        
        resolver.getSymbolsWithAnnotation(GenerateFlatMapExtensions::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { generateFlatMapExtensions(it) }
        
        return emptyList()
    }
    
    private fun KSClassDeclaration.getNewlyDeclaredProviderProperties(): Sequence<KSPropertyDeclaration> =
        getDeclaredProperties()
            .filter { it.isPublic() }
            .filter { it.findOverridee() == null }
            .filter { providerType.isAssignableFrom(it.type.resolve()) }
    
    private fun KSClassDeclaration.getNewlyDeclaredProviderFunctions(): Sequence<KSFunctionDeclaration> =
        getDeclaredFunctions()
            .filter { it.isPublic() }
            .filter { it.findOverridee() == null }
            .filter { providerType.isAssignableFrom(it.returnType?.resolve() ?: return@filter false) }
    
    private fun generateFlatMapExtensions(clazz: KSClassDeclaration) {
        val clazzName = clazz.simpleName.asString()
        val packageName = clazz.packageName.asString()
        val providerClass = ClassName("xyz.xenondevs.commons.provider", "Provider")
        
        val fileSpec = FileSpec.builder(packageName, "${clazzName}FlatMapExtensions")
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "unused")
                    .build()
            )
        
        for (property in clazz.getNewlyDeclaredProviderProperties()) {
            val propertyName = property.simpleName.asString()
            val providerValueType = property.type.resolve().arguments[0].type?.resolve()
            
            val receiverType = providerClass.parameterizedBy(clazz.toClassName())
            val returnType = providerClass.parameterizedBy(providerValueType!!.toTypeName())
            
            val propertySpec = PropertySpec.builder(propertyName, returnType)
                .receiver(receiverType)
                .addKdoc("Shortcut to [flatMap][Provider.flatMap] to [%L.%L].", clazzName, propertyName)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return flatMap { it.%L }", propertyName)
                        .build()
                )
                .build()
            
            fileSpec.addProperty(propertySpec)
        }
        
        for (function in clazz.getNewlyDeclaredProviderFunctions()) {
            val functionName = function.simpleName.asString()
            val providerValueType = function.returnType?.resolve()?.arguments?.get(0)?.type?.resolve()
            
            val receiverType = providerClass.parameterizedBy(clazz.toClassName())
            val returnType = providerClass.parameterizedBy(providerValueType!!.toTypeName())
            
            val parameters = function.parameters.map { param ->
                val paramName = param.name!!.asString()
                val paramType = param.type.toTypeName()
                
                ParameterSpec.builder(paramName, paramType).build()
            }
            
            val callArgs = function.parameters.joinToString(", ") { it.name!!.asString() }
            
            val funSpec = FunSpec.builder(functionName)
                .receiver(receiverType)
                .addParameters(parameters)
                .returns(returnType)
                .addKdoc("Shortcut to [flatMap][Provider.flatMap] to [%L.%L].", clazzName, functionName)
                .addStatement("return flatMap { it.%L(%L) }", functionName, callArgs)
                .build()
            
            fileSpec.addFunction(funSpec)
        }
        
        fileSpec.build().writeTo(codeGenerator, Dependencies(false, clazz.containingFile!!))
    }
    
}
