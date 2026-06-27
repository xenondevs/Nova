package xyz.xenondevs.nova.packetentity.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import java.io.File

class NovaPacketEntityProcessor(
    codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    private val entityDataGenerator = EntityDataGenerator()
    private val metadataDslGenerator = MetadataDslGenerator(codeGenerator, logger)
    private val packetEntityDslGenerator = PacketEntityDslGenerator(codeGenerator)

    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked)
            return emptyList()
        invoked = true

        val classpathStr = options[OPTION_CLASSPATH]
        if (classpathStr == null) {
            logger.error("Missing KSP option '$OPTION_CLASSPATH'. Configure it in build.gradle.kts via ksp { arg(\"$OPTION_CLASSPATH\", ...) }")
            return emptyList()
        }

        val classpathEntries = classpathStr
            .split(File.pathSeparatorChar)
            .map(::File)
            .filter { it.exists() }

        val result = entityDataGenerator.analyze(classpathEntries)
        metadataDslGenerator.generate(result, resolver)
        packetEntityDslGenerator.generate(result)

        return emptyList()
    }

    companion object {
        const val OPTION_CLASSPATH = "entityDataClasspath"
    }

}
