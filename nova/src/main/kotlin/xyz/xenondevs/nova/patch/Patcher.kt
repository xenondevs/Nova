package xyz.xenondevs.nova.patch

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import net.minecraft.server.MinecraftServer
import org.bukkit.Bukkit
import xyz.xenondevs.bytebase.ClassWrapperLoader
import xyz.xenondevs.bytebase.INSTRUMENTATION
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.patch.adapter.LcsWrapperAdapter
import xyz.xenondevs.nova.patch.impl.FieldFilterPatch
import xyz.xenondevs.nova.patch.impl.block.BlockBehaviorPatches
import xyz.xenondevs.nova.patch.impl.block.BlockMigrationPatches
import xyz.xenondevs.nova.patch.impl.block.DisableBackingStateLogicPatch
import xyz.xenondevs.nova.patch.impl.block.FluidFlowPatch
import xyz.xenondevs.nova.patch.impl.block.TripwireLogicPatch
import xyz.xenondevs.nova.patch.impl.bossbar.BossBarOriginPatch
import xyz.xenondevs.nova.patch.impl.chunk.ChunkSchedulingPatch
import xyz.xenondevs.nova.patch.impl.item.ArmorEquipEventPatch
import xyz.xenondevs.nova.patch.impl.item.DyeablePatches
import xyz.xenondevs.nova.patch.impl.item.EnchantmentPatches
import xyz.xenondevs.nova.patch.impl.item.FuelPatches
import xyz.xenondevs.nova.patch.impl.item.ItemStackDataComponentsPatch
import xyz.xenondevs.nova.patch.impl.item.RemainingItemPatches
import xyz.xenondevs.nova.patch.impl.item.RepairPatches
import xyz.xenondevs.nova.patch.impl.item.ToolPatches
import xyz.xenondevs.nova.patch.impl.misc.EventPreventionPatch
import xyz.xenondevs.nova.patch.impl.misc.FakePlayerLastHurtPatch
import xyz.xenondevs.nova.patch.impl.playerlist.BroadcastPacketPatch
import xyz.xenondevs.nova.patch.impl.registry.RegistryEventsPatch
import xyz.xenondevs.nova.patch.impl.sound.SoundPatches
import xyz.xenondevs.nova.patch.impl.worldgen.NovaRuleTestPatch
import xyz.xenondevs.nova.patch.impl.worldgen.WrapperBlockPatch
import xyz.xenondevs.nova.patch.impl.worldgen.chunksection.ChunkAccessSectionsPatch
import xyz.xenondevs.nova.patch.impl.worldgen.chunksection.LevelChunkSectionPatch
import xyz.xenondevs.nova.patch.impl.worldgen.registry.RegistryCodecPatch
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CLASS_LOADER_PARENT_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.reflection.defineClass
import java.lang.System.getProperty
import java.lang.instrument.ClassDefinition
import java.lang.management.ManagementFactory
import java.lang.reflect.Field

internal object Patcher {
    
    private val extraOpens = setOf("java.lang", "java.lang.reflect", "java.util", "jdk.internal.misc", "jdk.internal.reflect")
    private val transformers by lazy {
        sequenceOf(
            FieldFilterPatch, ToolPatches,
            LevelChunkSectionPatch, ChunkAccessSectionsPatch, RegistryCodecPatch,
            WrapperBlockPatch, NovaRuleTestPatch, FuelPatches, RemainingItemPatches, SoundPatches,
            BroadcastPacketPatch, EventPreventionPatch, ArmorEquipEventPatch, BossBarOriginPatch,
            FakePlayerLastHurtPatch, BlockBehaviorPatches, ChunkSchedulingPatch, DisableBackingStateLogicPatch,
            ItemStackDataComponentsPatch, EnchantmentPatches, RepairPatches, BlockMigrationPatches,
            TripwireLogicPatch, FluidFlowPatch, RegistryEventsPatch, DyeablePatches, EarlyBlockPlaceEventPatch
        ).filter(Transformer::shouldTransform).toSet()
    }
    
    // These class names can't be accessed via reflection to prevent class loading
    private val injectedClasses: Map<String, LcsWrapperAdapter?> = mapOf(
        "xyz/xenondevs/nova/patch/impl/worldgen/chunksection/LevelChunkSectionWrapper" to LcsWrapperAdapter
    )
    
    fun run() {
        try {
            LOGGER.info("Applying patches...")
            VirtualClassPath.classLoaders += javaClass.classLoader
            redefineModule()
            defineInjectedClasses()
            runTransformers()
            insertPatchedLoader()
            undoReversiblePatches()
        } catch (t: Throwable) {
            throw PatcherException(t)
        }
    }
    
    private fun redefineModule() {
        val novaModule = setOf(javaClass.module)
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
        //(javaClass.classLoader as NovaClassLoader).addInjectedClasses(injectedClasses.keys.map { it.replace('/', '.') })
        injectedClasses.forEach { (name, adapter) ->
            var bytes = javaClass.getResourceAsStream("/$name.class")!!.readBytes()
            if (bytes.isEmpty()) throw IllegalStateException("Failed to load injected class $name (Wrong path?)")
            val minecraftServerClass = MinecraftServer::class.java
            if (adapter != null) {
                val clazz = ClassWrapper("$name.class", bytes)
                adapter.adapt(clazz)
                bytes = clazz.assemble()
            }
            
            minecraftServerClass.classLoader.defineClass(name.replace('/', '.'), bytes, minecraftServerClass.protectionDomain)
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
            try {
                ClassDefinition(clazz, VirtualClassPath[clazz].assemble(computeFrames))
            } catch (t: Throwable) {
                throw AssembleException(clazz, findRelatedTransformers(clazz), t)
            }
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
                
                // tries to get more information by loading the classes using the ClassWrapperLoader instead of the instrumentation
                val classLoader = ClassWrapperLoader(javaClass.classLoader)
                try {
                    classLoader.loadClass(VirtualClassPath[defClass]).methods
                } catch (e: LinkageError) {
                    throw RedefineException(defClass, findRelatedTransformers(defClass), "Type: ${e::class.simpleName}\n${e.message}")
                } catch (e: Throwable) {
                    // throws generic patching exception below
                }
                
                throw RedefineException(defClass, findRelatedTransformers(defClass), ex)
            }
        }
    }
    
    private fun findRelatedTransformers(clazz: Class<*>): List<Transformer> =
        transformers.filter { tf -> tf.classes.any { tfClass -> tfClass.internalName == clazz.internalName } }
    
    private fun insertPatchedLoader() {
        val spigotLoader = Bukkit::class.java.classLoader
        ReflectionUtils.setFinalField(CLASS_LOADER_PARENT_FIELD, spigotLoader, PatchedClassLoader())
    }
    
}

private class PatcherException(t: Throwable) : Exception("""
    JDK: ${getProperty("java.version")} by ${getProperty("java.vendor")}
    JVM: ${getProperty("java.vm.name")}, ${getProperty("java.vm.version")} by ${getProperty("java.vm.vendor")}
    Operating system: ${getProperty("os.name")}, ${getProperty("os.arch")}
    Startup parameters: ${ManagementFactory.getRuntimeMXBean().inputArguments}
""", t)

private class RedefineException : Exception {
    
    constructor(defClass: Class<*>, transformers: List<Transformer>, message: String) : this("Failed to redefine classes", defClass, transformers, message)
    constructor(defClass: Class<*>, transformers: List<Transformer>, t: Throwable) : this("Failed to redefine classes. Could not get more information.", defClass, transformers, "", t)
    
    private constructor(m1: String, defClass: Class<*>, transformers: List<Transformer>, m2: String, t: Throwable? = null) :
        super("$m1\nClass: ${defClass.internalName}\nRelated transformers: ${transformers.joinToString()}\n$m2", t)
    
}

private class AssembleException(clazz: Class<*>, transformers: List<Transformer>, t: Throwable) :
    Exception("Failed to assemble class ${clazz.internalName}\nRelated transformers: ${transformers.joinToString()}", t)