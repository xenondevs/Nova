package xyz.xenondevs.nova.registry.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class NovaRegistryProcessor(
    codeGenerator: CodeGenerator
) : SymbolProcessor {
    
    private val entriesGenerator = EntriesGenerator(codeGenerator)
    private val extensionsGenerator = ExtensionsGenerator(codeGenerator)
    private val serializersGenerator = SerializersGenerator(codeGenerator)
    private val typeAliasesGenerator = TypeAliasesGenerator(codeGenerator)
    private val registryKeysGenerator = RegistryKeysGenerator(codeGenerator)
    
    private var invoked = false
    
    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked)
            return emptyList()
        invoked = true
        
        resolver.getDeclarationsFromPackage("io.papermc.paper.registry.keys")
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.simpleName.asString().endsWith("Keys") }
            .forEach { entriesGenerator.generateEntriesFile(it) }
        
        resolver.getDeclarationsFromPackage("io.papermc.paper.registry.keys.tags")
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.simpleName.asString().endsWith("TagKeys") }
            .forEach { entriesGenerator.generateEntrySetsFile(it) }
        
        val registryKey = resolver.getClassDeclarationByName("io.papermc.paper.registry.RegistryKey")!!
        extensionsGenerator.generateTypedKeyExtensions(registryKey)
        extensionsGenerator.generateEntryExtensions(registryKey)
        serializersGenerator.generatePaperRegistrySerializers(registryKey)
        typeAliasesGenerator.generateSerializableTypeAliases(registryKey)
        registryKeysGenerator.generateAllRegistryKeys(registryKey)
        
        return emptyList()
    }
    
}
