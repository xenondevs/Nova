package xyz.xenondevs.nova.ksp

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class NovaKspProcessorProvider : SymbolProcessorProvider {
    
    override fun create(
        environment: SymbolProcessorEnvironment
    ) = FlatMapExtensionProcessor(
        codeGenerator = environment.codeGenerator
    )
    
}
