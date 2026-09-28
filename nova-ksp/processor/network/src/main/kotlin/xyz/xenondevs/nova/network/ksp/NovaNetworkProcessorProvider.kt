package xyz.xenondevs.nova.network.ksp

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class NovaNetworkProcessorProvider : SymbolProcessorProvider {
    
    override fun create(
        environment: SymbolProcessorEnvironment
    ) = NovaNetworkProcessor(
        codeGenerator = environment.codeGenerator
    )
    
}
