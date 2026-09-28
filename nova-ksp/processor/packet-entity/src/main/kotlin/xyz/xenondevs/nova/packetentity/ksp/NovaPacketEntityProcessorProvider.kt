package xyz.xenondevs.nova.packetentity.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class NovaPacketEntityProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return NovaPacketEntityProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
    }

}
