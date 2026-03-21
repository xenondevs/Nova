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
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import java.io.OutputStream

class NovaRegistryProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    
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
        val keysQualifiedName = keys.qualifiedName!!.asString()
        val keysClassName = keys.simpleName.getShortName()
        val typePrettyName = keysClassName.removeSuffix("Keys")
        codeGenerator.createNewFile(
            dependencies = Dependencies(false, keys.containingFile!!),
            packageName = "xyz.xenondevs.nova.registry.entries",
            fileName = "${typePrettyName}Entries"
        ).use { file ->
            file += """
                @file:Suppress("unused")
                
                package xyz.xenondevs.nova.registry.entries
                
                import $keysQualifiedName
                import xyz.xenondevs.nova.registry.RegistryEntry
                
                /**
                * Contains corresponding entries for keys in [${keysClassName}].
                */
                object ${typePrettyName}Entries {
            """.trimIndent()
            
            keys.getDeclaredProperties()
                .filter { Modifier.PUBLIC in it.modifiers && Modifier.JAVA_STATIC in it.modifiers }
                .filter { (it.type.element as KSClassifierReference).referencedName() == "TypedKey" }
                .forEach { prop ->
                    val name = prop.simpleName.getShortName()
                    val typeDecl = prop.type.resolve().toQualifiedString() // TypedKey<GameRule<*>> -> GameRule<*>
                        .substringAfter('<')
                        .substringBeforeLast('>')
                    file += """
                        /**
                        * An entry for [${keysClassName}.$name].
                        */
                        @JvmField
                        val $name: RegistryEntry.Paper<$typeDecl> = 
                            RegistryEntry.of($keysClassName.$name)
                            
                     """.trimIndent().prependIndent()
                }
            
            file += "}"
        }
    }
    
    private fun generateEntrySetsFile(tagKeys: KSClassDeclaration) {
        val tagKeysQualifiedName = tagKeys.qualifiedName!!.asString()
        val tagKeysClassName = tagKeys.simpleName.getShortName()
        val typePrettyName = tagKeysClassName.removeSuffix("TagKeys")
        codeGenerator.createNewFile(
            dependencies = Dependencies(false, tagKeys.containingFile!!),
            packageName = "xyz.xenondevs.nova.registry.entries",
            fileName = "${typePrettyName}Tags"
        ).use { file ->
            file += """
                @file:Suppress("unused")
                
                package xyz.xenondevs.nova.registry.entries
                
                import $tagKeysQualifiedName
                import xyz.xenondevs.nova.registry.RegistryEntrySet
                import xyz.xenondevs.nova.registry.registryEntrySetOf
                
                /**
                * Contains corresponding entry sets for tags in [${tagKeysClassName}].
                */
                object ${typePrettyName}Tags {
            """.trimIndent()
            
            tagKeys.getDeclaredProperties()
                .filter { Modifier.PUBLIC in it.modifiers && Modifier.JAVA_STATIC in it.modifiers }
                .filter { (it.type.element as KSClassifierReference).referencedName() == "TagKey" }
                .forEach { prop ->
                    val name = prop.simpleName.getShortName()
                    val typeDecl = prop.type.resolve().toQualifiedString() // TypedKey<GameRule<*>> -> GameRule<*>
                        .substringAfter('<')
                        .substringBeforeLast('>')
                    file += """
                        /**
                        * An entry set containing the tag [${tagKeysClassName}.$name].
                        */
                        @JvmField
                        val $name: RegistryEntrySet.Paper.Tag<$typeDecl> = 
                            registryEntrySetOf(${tagKeysClassName}.$name)
                            
                    """.trimIndent().prependIndent()
                }
            
            file += "}"
        }
    }
    
    private fun generateTypedKeyExtensions(registryKey: KSClassDeclaration) {
        codeGenerator.createNewFile(
            dependencies = Dependencies(false, registryKey.containingFile!!),
            packageName = "xyz.xenondevs.nova.registry",
            fileName = "TypedKeyExtensions"
        ).use { file ->
            file += """
                @file:Suppress("unused")
                
                package xyz.xenondevs.nova.registry
                
                import io.papermc.paper.registry.RegistryKey
                import io.papermc.paper.registry.TypedKey
                
            """.trimIndent()
            
            registryKey.getDeclaredProperties()
                .filter { Modifier.PUBLIC in it.modifiers && Modifier.JAVA_STATIC in it.modifiers }
                .filter { (it.type.element as KSClassifierReference).referencedName() == "RegistryKey" }
                .filter { property ->
                    // excludes registry keys whose type's getKey method is deprecated (these can exist without a key)
                    val keyType = property.type.element!!.typeArguments[0].type!!.resolve()
                    val keyTypeDecl = keyType.declaration as KSClassDeclaration
                    val getKeyMethod = keyTypeDecl.getAllFunctions().first { it.simpleName.asString() == "getKey" }
                    !getKeyMethod.isAnnotationPresent(Deprecated::class)
                }
                .forEach { property ->
                    val keyName = property.simpleName.asString()
                    val keyType = property.type.element!!.typeArguments[0].type!!.resolve()
                    val keyQualifiedName = keyType.declaration.qualifiedName!!.asString()
                    val keyFullQualifiedName = keyType.toQualifiedString()
                    file += """
                        /**
                        * Gets the typed key for this [$keyQualifiedName] in [RegistryKey.$keyName].
                        */
                        val $keyFullQualifiedName.typedKey: TypedKey<$keyFullQualifiedName>
                            get() = TypedKey.create(RegistryKey.$keyName, key)
                        
                    """.trimIndent()
                }
        }
    }
    
    private fun generateEntryExtensions(registryKey: KSClassDeclaration) {
        codeGenerator.createNewFile(
            dependencies = Dependencies(false, registryKey.containingFile!!),
            packageName = "xyz.xenondevs.nova.registry",
            fileName = "EntryExtensions"
        ).use { file ->
            file += """
                @file:Suppress("unused")
                
                package xyz.xenondevs.nova.registry
                
                import io.papermc.paper.registry.RegistryKey
                import io.papermc.paper.registry.TypedKey
                
            """.trimIndent()
            
            registryKey.getDeclaredProperties()
                .filter { Modifier.PUBLIC in it.modifiers && Modifier.JAVA_STATIC in it.modifiers }
                .filter { (it.type.element as KSClassifierReference).referencedName() == "RegistryKey" }
                .filter { property ->
                    // excludes registry keys whose type's getKey method is deprecated (these can exist without a key)
                    val keyType = property.type.element!!.typeArguments[0].type!!.resolve()
                    val keyTypeDecl = keyType.declaration as KSClassDeclaration
                    val getKeyMethod = keyTypeDecl.getAllFunctions().first { it.simpleName.asString() == "getKey" }
                    !getKeyMethod.isAnnotationPresent(Deprecated::class)
                }
                .forEach { property ->
                    val keyName = property.simpleName.asString()
                    val keyType = property.type.element!!.typeArguments[0].type!!.resolve()
                    val keyQualifiedName = keyType.declaration.qualifiedName!!.asString()
                    val keyFullQualifiedName = keyType.toQualifiedString()
                    file += """
                        /**
                        * Gets the registry entry for this [$keyQualifiedName] in [RegistryKey.$keyName].
                        */
                        val $keyFullQualifiedName.entry: RegistryEntry.Paper<$keyFullQualifiedName>
                            get() = RegistryEntry.of(RegistryKey.$keyName, this)
                        
                    """.trimIndent()
                }
        }
    }
    
}

private operator fun OutputStream.plusAssign(str: String) {
    write(str.toByteArray())
    write("\n".toByteArray())
}

private fun KSType.toQualifiedString(): String {
    val declarationName = declaration.qualifiedName?.asString()
        ?: declaration.simpleName.asString()
    
    if (arguments.isEmpty())
        return declarationName
    
    val args = arguments.joinToString(", ") { typeArg ->
        typeArg.type?.resolve()?.toQualifiedString() ?: "*"
    }
    
    return "$declarationName<$args>"
}