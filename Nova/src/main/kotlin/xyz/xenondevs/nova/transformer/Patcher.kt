package xyz.xenondevs.nova.transformer

import xyz.xenondevs.bytebase.INSTRUMENTATION
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.transformer.patch.noteblock.NoteBlockPatch
import xyz.xenondevs.nova.util.ServerUtils
import java.lang.instrument.ClassDefinition

internal object Patcher : Initializable() {
    override val inMainThread = false
    override val dependsOn = emptySet<Initializable>()
    
    val transformers = setOf(NoteBlockPatch)
    
    override fun init() {
        if (ServerUtils.isReload || !DEFAULT_CONFIG.getBoolean("use_agent"))
            return
        
        val classes = transformers.groupBy { it.clazz.java }.map { it.key to it.value.any(ClassTransformer::computeFrames) }
        transformers.forEach(ClassTransformer::transform)
        val definitions = classes.map { (clazz, computeFrames) ->
            ClassDefinition(clazz, VirtualClassPath[clazz].assemble(computeFrames))
        }.toTypedArray()
        INSTRUMENTATION.redefineClasses(*definitions)
    }
}