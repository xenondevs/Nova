package xyz.xenondevs.nova.registry.ksp

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class NovaRegistryProcessorProvider : SymbolProcessorProvider {
    
    override fun create(
        environment: SymbolProcessorEnvironment
    ) = NovaRegistryProcessor(
        codeGenerator = environment.codeGenerator
    )
    
}