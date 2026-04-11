package xyz.xenondevs.nova.network.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

class NovaNetworkProcessor(
    codeGenerator: CodeGenerator
) : SymbolProcessor {
    
    private val packetIdsGenerator = PacketIdsGenerator(codeGenerator)
    private val packetEventGenerator = PacketEventGenerator(codeGenerator)
    
    private var invoked = false
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked)
            return emptyList()
        invoked = true
        
        packetIdsGenerator.generate(resolver)
        packetEventGenerator.generate(resolver)
        
        return emptyList()
    }
    
}
