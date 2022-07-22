package xyz.xenondevs.nova.transformer

import xyz.xenondevs.bytebase.INSTRUMENTATION
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.transformer.patch.FieldFilterPatch
import xyz.xenondevs.nova.transformer.patch.noteblock.NoteBlockPatch
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.lang.instrument.ClassDefinition
import java.lang.reflect.Field

internal object Patcher : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = emptySet<Initializable>()
    
    private val extraOpens = setOf("java.lang", "jdk.internal.misc", "jdk.internal.reflect")
    private val transformers by lazy { setOf(NoteBlockPatch, FieldFilterPatch) }
    
    override fun init() {
        if (!DEFAULT_CONFIG.getBoolean("use_agent"))
            return
        
        if (runCatching { INSTRUMENTATION }.isFailure) {
            LOGGER.warning("Java agents aren't supported on this server! Disabling...")
            DEFAULT_CONFIG["use_agent"] = false
            NovaConfig.save("config")
            return
        }
        
        LOGGER.info("Performing patches...")
        VirtualClassPath.classLoaders += Nova::class.java.classLoader.parent
        redefineModule()
        runTransformers()
        insertPatchedLoader()
    }
    
    override fun disable() {
        if (DEFAULT_CONFIG.getBoolean("use_agent"))
            removePatchedLoader()
    }
    
    private fun redefineModule() {
        val myModule = setOf(Nova::class.java.module)
        INSTRUMENTATION.redefineModule(
            Field::class.java.module, // java.base module
            emptySet(),
            emptyMap(),
            extraOpens.associateWith { myModule },
            emptySet(),
            emptyMap()
        )
    }
    
    private fun runTransformers() {
        val classes = transformers.groupBy { it.clazz.java }.map { it.key to it.value.any(ClassTransformer::computeFrames) }
        transformers.forEach(ClassTransformer::transform)
        val definitions = classes.map { (clazz, computeFrames) ->
            ClassDefinition(clazz, VirtualClassPath[clazz].assemble(computeFrames))
        }.toTypedArray()
        INSTRUMENTATION.redefineClasses(*definitions)
    }
    
    private fun insertPatchedLoader() {
        val spigotLoader = Nova::class.java.classLoader.parent
        val parentField = ReflectionUtils.getField(ClassLoader::class.java, true, "parent")
        ReflectionUtils.setFinalField(parentField, spigotLoader, PatchedClassLoader())
    }
    
    private fun removePatchedLoader() {
        val spigotLoader = Nova::class.java.classLoader.parent
        val parentField = ReflectionUtils.getField(ClassLoader::class.java, true, "parent")
        ReflectionUtils.setFinalField(parentField, spigotLoader, spigotLoader.parent.parent)
    }
    
}