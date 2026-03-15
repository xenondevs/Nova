@file:OptIn(KspExperimental::class)

package xyz.xenondevs.nova.registry.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class NovaRegistryProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    
    private val registryPackage = "xyz.xenondevs.nova.registry"
    private val entriesPackage = "$registryPackage.entries"
    private val paperRegistryPackage = "io.papermc.paper.registry"
    private val registryEntryClass = ClassName(registryPackage, "RegistryEntry")
    private val registryEntryPaperClass = registryEntryClass.nestedClass("Paper")
    private val registryEntrySetPaperTagClass = ClassName(registryPackage, "RegistryEntrySet")
        .nestedClass("Paper")
        .nestedClass("Tag")
    private val typedKeyClass = ClassName(paperRegistryPackage, "TypedKey")
    private val registryKeyClass = ClassName(paperRegistryPackage, "RegistryKey")
    private val registryEntrySetOfMember = MemberName(registryPackage, "registryEntrySetOf")
    private val jvmFieldAnnotation = AnnotationSpec.builder(JvmField::class).build()
    private val suppressUnusedAnnotation = AnnotationSpec.builder(Suppress::class)
        .addMember("%S", "unused")
        .build()
    
    private var invoked = false
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked)
            return emptyList()
        invoked = true
        
        resolver.getDeclarationsFromPackage("io.papermc.paper.registry.keys")
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.simpleName.asString().endsWith("Keys") }
            .forEach { generateEntriesFile(it) }
        
        resolver.getDeclarationsFromPackage("io.papermc.paper.registry.keys.tags")
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.simpleName.asString().endsWith("TagKeys") }
            .forEach { generateEntrySetsFile(it) }
        
        val registryKey = resolver.getClassDeclarationByName("io.papermc.paper.registry.RegistryKey")!!
        generateTypedKeyExtensions(registryKey)
        generateEntryExtensions(registryKey)
        
        return emptyList()
    }
    
    private fun generateEntriesFile(keys: KSClassDeclaration) {
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
    
    private fun generateEntrySetsFile(tagKeys: KSClassDeclaration) {
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
    
    private fun generateTypedKeyExtensions(registryKey: KSClassDeclaration) {
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
    
    private fun generateEntryExtensions(registryKey: KSClassDeclaration) {
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

private fun KSClassDeclaration.publicStaticPropertiesOfType(typeName: String): Sequence<KSPropertyDeclaration> =
    getDeclaredProperties()
        .filter { Modifier.PUBLIC in it.modifiers && Modifier.JAVA_STATIC in it.modifiers }
        .filter { (it.type.element as? KSClassifierReference)?.referencedName() == typeName }

private fun KSPropertyDeclaration.primaryTypeArgument(): KSType =
    type.element!!.typeArguments.first().type!!.resolve()

private fun KSClassDeclaration.nonDeprecatedRegistryKeyProperties(): Sequence<KSPropertyDeclaration> =
    publicStaticPropertiesOfType("RegistryKey")
        .filter { property ->
            // Excludes registry keys whose type's getKey method is deprecated (these can exist without a key).
            val keyTypeDecl = property.primaryTypeArgument().declaration as KSClassDeclaration
            val getKeyMethod = keyTypeDecl.getAllFunctions().first { it.simpleName.asString() == "getKey" }
            !getKeyMethod.isAnnotationPresent(Deprecated::class)
        }
