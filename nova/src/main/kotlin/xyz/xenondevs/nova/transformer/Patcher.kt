package xyz.xenondevs.nova.transformer

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import net.minecraft.server.MinecraftServer
import xyz.xenondevs.bytebase.ClassWrapperLoader
import xyz.xenondevs.bytebase.INSTRUMENTATION
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.loader.NovaClassLoader
import xyz.xenondevs.nova.transformer.patch.FieldFilterPatch
import xyz.xenondevs.nova.transformer.patch.block.NoteBlockPatch
import xyz.xenondevs.nova.transformer.patch.bossbar.BossBarOriginPatch
import xyz.xenondevs.nova.transformer.patch.event.FakePlayerEventPreventionPatch
import xyz.xenondevs.nova.transformer.patch.item.AnvilResultPatch
import xyz.xenondevs.nova.transformer.patch.item.AttributePatch
import xyz.xenondevs.nova.transformer.patch.item.DamageablePatches
import xyz.xenondevs.nova.transformer.patch.item.EnchantmentPatches
import xyz.xenondevs.nova.transformer.patch.item.FireResistancePatches
import xyz.xenondevs.nova.transformer.patch.item.FuelPatches
import xyz.xenondevs.nova.transformer.patch.item.LegacyConversionPatch
import xyz.xenondevs.nova.transformer.patch.item.RemainingItemPatches
import xyz.xenondevs.nova.transformer.patch.item.StackSizePatch
import xyz.xenondevs.nova.transformer.patch.item.ToolPatches
import xyz.xenondevs.nova.transformer.patch.item.WearablePatch
import xyz.xenondevs.nova.transformer.patch.nbt.CBFCompoundTagPatch
import xyz.xenondevs.nova.transformer.patch.playerlist.BroadcastPacketPatch
import xyz.xenondevs.nova.transformer.patch.sound.SoundPatches
import xyz.xenondevs.nova.transformer.patch.worldgen.FeatureSorterPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.NovaRuleTestPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.WrapperBlockPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.chunksection.ChunkAccessSectionsPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.chunksection.LevelChunkSectionPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.registry.MappedRegistryPatch
import xyz.xenondevs.nova.transformer.patch.worldgen.registry.RegistryCodecPatch
import xyz.xenondevs.nova.util.ServerUtils
import xyz.xenondevs.nova.util.data.getResourceData
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.reflection.defineClass
import java.lang.System.getProperty
import java.lang.instrument.ClassDefinition
import java.lang.management.ManagementFactory
import java.lang.reflect.Field

internal object Patcher : Initializable() {
    
    override val initializationStage = InitializationStage.PRE_WORLD
    override val dependsOn = emptySet<Initializable>()
    
    private val extraOpens = setOf("java.lang", "java.lang.reflect", "java.util", "jdk.internal.misc", "jdk.internal.reflect")
    private val transformers by lazy {
        sequenceOf(
            FieldFilterPatch, NoteBlockPatch, DamageablePatches, ToolPatches, AttributePatch, EnchantmentPatches, AnvilResultPatch,
            StackSizePatch, FeatureSorterPatch, LevelChunkSectionPatch, ChunkAccessSectionsPatch, RegistryCodecPatch,
            WrapperBlockPatch, MappedRegistryPatch, FuelPatches, RemainingItemPatches, FireResistancePatches, SoundPatches,
            BroadcastPacketPatch, CBFCompoundTagPatch, FakePlayerEventPreventionPatch, LegacyConversionPatch, WearablePatch,
            NovaRuleTestPatch, BossBarOriginPatch
        ).filter(Transformer::shouldTransform).toSet()
    }
    
    // These class names can't be accessed via reflection to prevent class loading
    private val injectedClasses = setOf(
        "xyz/xenondevs/nova/transformer/patch/worldgen/chunksection/LevelChunkSectionWrapper"
    )
    
    private lateinit var classLoaderParentField: Field
    
    override fun init() {
        try {
            LOGGER.info("Applying patches...")
            VirtualClassPath.classLoaders += NOVA.loader.javaClass.classLoader.parent
            redefineModule()
            defineInjectedClasses()
            runTransformers()
            classLoaderParentField = ReflectionUtils.getField(ClassLoader::class.java, true, "parent")
            insertPatchedLoader()
            undoReversiblePatches()
        } catch (t: Throwable) {
            throw PatcherException(t)
        }
    }
    
    override fun disable() {
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
    
    private fun defineInjectedClasses() {
        (javaClass.classLoader as NovaClassLoader).addInjectedClasses(injectedClasses.map { it.replace('/', '.') })
        if (ServerUtils.isReload) return
        injectedClasses.forEach {
            val bytes = getResourceData("$it.class")
            if (bytes.isEmpty()) throw IllegalStateException("Failed to load injected class $it (Wrong path?)")
            val minecraftServerClass = MinecraftServer::class.java
            minecraftServerClass.classLoader.defineClass(it.replace('/', '.'), bytes, minecraftServerClass.protectionDomain)
        }
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
        // I don't know why, but when redefining all classes at once and one fails to load, the extra information retrieved
        // using the ClassWrapperLoader is completely different and consists of errors that don't happen when using the instrumentation.
        // Looping over all class definitions individually seems to fix this issue.
        for (definition in definitions) {
            try {
                INSTRUMENTATION.redefineClasses(definition)
            } catch (ex: LinkageError) {
                val defClass = definition.definitionClass
                val relatedPatches = transformers.filter { tf -> tf.classes.any { tfClass -> tfClass.internalName == defClass.internalName } }
                
                // tries to get more information by loading the classes using the ClassWrapperLoader instead of the instrumentation
                val classLoader = ClassWrapperLoader(javaClass.classLoader)
                try {
                    classLoader.loadClass(VirtualClassPath[defClass]).methods
                } catch (e: LinkageError) {
                    throw PatchingException(defClass, relatedPatches, "Type: ${e::class.simpleName}\n${e.message}")
                } catch (e: Throwable) {
                    // throws generic patching exception below
                }
                
                throw PatchingException(defClass, relatedPatches, ex)
            }
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

private class PatcherException(t: Throwable) : Exception("""
    JDK: ${getProperty("java.version")} by ${getProperty("java.vendor")}
    JVM: ${getProperty("java.vm.name")}, ${getProperty("java.vm.version")} by ${getProperty("java.vm.vendor")}
    Operating system: ${getProperty("os.name")}, ${getProperty("os.arch")}
    Startup parameters: ${ManagementFactory.getRuntimeMXBean().inputArguments}
""", t)

private class PatchingException : Exception {
    
    constructor(defClass: Class<*>, transformers: List<Transformer>, message: String) : this("Failed to apply patches", defClass, transformers, message)
    constructor(defClass: Class<*>, transformers: List<Transformer>, t: Throwable) : this("Failed to apply patches. Could not get more information.", defClass, transformers, "", t)
    
    private constructor(m1: String, defClass: Class<*>, transformers: List<Transformer>, m2: String, t: Throwable? = null) :
        super("$m1\nClass: ${defClass.internalName}\nRelated transformers: ${transformers.joinToString()}\n$m2", t)
    
}