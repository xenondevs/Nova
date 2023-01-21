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
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.transformer.patch.FieldFilterPatch
import xyz.xenondevs.nova.transformer.patch.event.FakePlayerEventPreventionPatch
import xyz.xenondevs.nova.transformer.patch.item.AnvilResultPatch
import xyz.xenondevs.nova.transformer.patch.item.AttributePatch
import xyz.xenondevs.nova.transformer.patch.item.DamageablePatches
import xyz.xenondevs.nova.transformer.patch.item.EnchantmentPatches
import xyz.xenondevs.nova.transformer.patch.item.FireResistancePatches
import xyz.xenondevs.nova.transformer.patch.item.FuelPatches
import xyz.xenondevs.nova.transformer.patch.item.RemainingItemPatches
import xyz.xenondevs.nova.transformer.patch.item.StackSizePatch
import xyz.xenondevs.nova.transformer.patch.item.ToolPatches
import xyz.xenondevs.nova.transformer.patch.nbt.CBFCompoundTagPatch
import xyz.xenondevs.nova.transformer.patch.noteblock.NoteBlockPatch
import xyz.xenondevs.nova.transformer.patch.playerlist.BroadcastPacketPatch
import xyz.xenondevs.nova.transformer.patch.sound.SoundPatches
import xyz.xenondevs.nova.util.mapToArray
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.lang.System.getProperty
import java.lang.instrument.ClassDefinition
import java.lang.management.ManagementFactory
import java.lang.reflect.Field
import java.util.logging.Level
import kotlin.reflect.jvm.jvmName

internal object Patcher : Initializable() {
    
    override val initializationStage = InitializationStage.PRE_WORLD
    override val dependsOn = emptySet<Initializable>()
    
    private val extraOpens = setOf("java.lang", "java.lang.reflect", "java.util", "jdk.internal.misc", "jdk.internal.reflect")
    private val transformers by lazy {
        sequenceOf(
            FieldFilterPatch, NoteBlockPatch, DamageablePatches, ToolPatches, AttributePatch, EnchantmentPatches, AnvilResultPatch,
            StackSizePatch, FuelPatches, RemainingItemPatches, FireResistancePatches, SoundPatches, BroadcastPacketPatch,
            CBFCompoundTagPatch, FakePlayerEventPreventionPatch
        ).filter(Transformer::shouldTransform).toSet()
    }
    
    private lateinit var classLoaderParentField: Field
    
    override fun init() {
        try {
            LOGGER.info("Applying patches...")
            VirtualClassPath.classLoaders += NOVA.loader.javaClass.classLoader.parent
            redefineModule()
            runTransformers()
            classLoaderParentField = ReflectionUtils.getField(ClassLoader::class.java, true, "parent")
            insertPatchedLoader()
            undoReversiblePatches()
        } catch (t: Throwable) {
            throw PatcherException(t)
        }
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
            val errorMessages = ArrayList<String>()
            
            // tries to get more information by loading the classes using the ClassWrapperLoader instead of the instrumentation
            val classLoader = ClassWrapperLoader(javaClass.classLoader)
            definitions.forEach {
                try {
                    classLoader.loadClass(VirtualClassPath[it.definitionClass]).methods
                } catch (e: LinkageError) {
                    if (e.message?.contains(ClassWrapperLoader::class.jvmName) != true) {
                        errorMessages += "${e::class.simpleName} for class ${it.definitionClass.internalName}:\n${e.message}"
                    }
                } catch (t: Throwable) {
                    LOGGER.log(Level.SEVERE, "Failed to load class while trying to obtain more information: ${it.definitionClass.internalName}", t)
                }
            }
            
            if (errorMessages.size > 0) {
                throw PatchingException(errorMessages)
            } else throw PatchingException(ex)
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
    
    constructor(messages: List<String>) : super("Failed to apply patches:\n${messages.joinToString("\n\n")}")
    constructor(t: Throwable) : super("Failed to apply patches. Could not get more information.", t)
    
}