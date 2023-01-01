package xyz.xenondevs.nova.transformer

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import xyz.xenondevs.bytebase.ClassWrapperLoader
import xyz.xenondevs.bytebase.INSTRUMENTATION
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.transformer.patch.FieldFilterPatch
import xyz.xenondevs.nova.transformer.patch.block.BlockSoundPatches
import xyz.xenondevs.nova.transformer.patch.item.AnvilResultPatch
import xyz.xenondevs.nova.transformer.patch.item.AttributePatch
import xyz.xenondevs.nova.transformer.patch.item.DamageablePatches
import xyz.xenondevs.nova.transformer.patch.item.EnchantmentPatches
import xyz.xenondevs.nova.transformer.patch.item.StackSizePatch
import xyz.xenondevs.nova.transformer.patch.item.ToolPatches
import xyz.xenondevs.nova.transformer.patch.noteblock.NoteBlockPatch
import xyz.xenondevs.nova.transformer.patch.playerlist.BroadcastPacketPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.FeatureSorterPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.registry.MappedRegistryPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.registry.RegistryCodecPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.WrapperBlockPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.chunksection.ChunkAccessSectionsPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.chunksection.LevelChunkSectionPatch
import xyz.xenondevs.nova.util.mapToArray
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.lang.instrument.ClassDefinition
import java.lang.reflect.Field
import java.util.logging.Level
import kotlin.reflect.jvm.jvmName
import kotlin.system.exitProcess

internal object Patcher : Initializable() {
    
    val ENABLED by configReloadable { DEFAULT_CONFIG.getBoolean("use_agent") }
    
    override val initializationStage = InitializationStage.PRE_WORLD
    override val dependsOn = emptySet<Initializable>()
    
    private val extraOpens = setOf("java.lang", "java.lang.reflect", "java.util", "jdk.internal.misc", "jdk.internal.reflect")
    private val transformers by lazy {
        sequenceOf(
            FieldFilterPatch, NoteBlockPatch, DamageablePatches, ToolPatches, AttributePatch, EnchantmentPatches, AnvilResultPatch,
            StackSizePatch, BlockSoundPatches, BroadcastPacketPatch, FeatureSorterPatch, LevelChunkSectionPatch, ChunkAccessSectionsPatch,
            RegistryCodecPatch, WrapperBlockPatch, MappedRegistryPatch
        ).filter(Transformer::shouldTransform).toSet()
    }
    
    private lateinit var classLoaderParentField: Field
    
    override fun init() {
        if (!ENABLED) {
            LOGGER.warning("Java agent is disabled. Some features will not work properly or at all.")
            return
        }
        
        if (runCatching { INSTRUMENTATION }.isFailure) {
            LOGGER.warning("Java agents aren't supported on this server! Disabling...")
            DEFAULT_CONFIG["use_agent"] = false
            NovaConfig.save("config")
            return
        }
        
        LOGGER.info("Applying patches...")
        VirtualClassPath.classLoaders += NOVA.loader.javaClass.classLoader.parent
        redefineModule()
        runTransformers()
        classLoaderParentField = ReflectionUtils.getField(ClassLoader::class.java, true, "parent")
        insertPatchedLoader()
        undoReversiblePatches()
    }
    
    override fun disable() {
        if (DEFAULT_CONFIG.getBoolean("use_agent"))
            removePatchedLoader()
    }
    
    private fun redefineModule() {
        val novaModule = setOf(Nova::class.java.module)
        val javaBase = Field::class.java.module
        
        INSTRUMENTATION.redefineModule(
            javaBase,
            emptySet(),
            emptyMap(),
            extraOpens.associateWith { novaModule },
            emptySet(),
            emptyMap()
        )
    }
    
    private fun runTransformers() {
        val classes = Object2BooleanOpenHashMap<Class<*>>() // class -> computeFrames
        transformers.forEach { transformer ->
            transformer.classes.forEach { clazz ->
                if (transformer.computeFrames)
                    classes[clazz.java] = true
                else classes.putIfAbsent(clazz.java, false)
            }
        }
        transformers.forEach(Transformer::transform)
        val definitions = classes.map { (clazz, computeFrames) ->
            ClassDefinition(clazz, VirtualClassPath[clazz].assemble(computeFrames))
        }.toTypedArray()
        
        redefineClasses(definitions)
    }
    
    private fun undoReversiblePatches() {
        val definitions = transformers.filterIsInstance<ReversibleClassTransformer>()
            .mapToArray { ClassDefinition(it.clazz.java, it.initialBytecode) }
        
        redefineClasses(definitions)
    }
    
    private fun redefineClasses(definitions: Array<ClassDefinition>) {
        try {
            INSTRUMENTATION.redefineClasses(*definitions)
        } catch (ex: LinkageError) {
            LOGGER.severe("Failed to apply patches (LinkageError: $ex)! Trying to get more information...")
            
            var thrown = false
            val classLoader = ClassWrapperLoader(javaClass.classLoader)
            definitions.forEach {
                try {
                    classLoader.loadClass(VirtualClassPath[it.definitionClass]).methods
                } catch (e: LinkageError) {
                    if (e.message?.contains(ClassWrapperLoader::class.jvmName) != true) {
                        LOGGER.severe("${e::class.simpleName} for class ${it.definitionClass.internalName}:\n${e.message}")
                        thrown = true
                    }
                } catch (t: Throwable) {
                    LOGGER.log(Level.SEVERE, "Failed to load class ${it.definitionClass.internalName}", t)
                }
            }
            
            if (!thrown) {
                LOGGER.log(Level.SEVERE, "Could not get more information, original stacktrace: ", ex)
            }
            
            LOGGER.severe("Exiting server process...")
            exitProcess(-1)
        }
    }
    
    private fun insertPatchedLoader() {
        val spigotLoader = NOVA.loader.javaClass.classLoader.parent
        ReflectionUtils.setFinalField(classLoaderParentField, spigotLoader, PatchedClassLoader())
    }
    
    private fun removePatchedLoader() {
        val spigotLoader = NOVA.loader.javaClass.classLoader.parent
        ReflectionUtils.setFinalField(classLoaderParentField, spigotLoader, spigotLoader.parent.parent)
    }
    
}