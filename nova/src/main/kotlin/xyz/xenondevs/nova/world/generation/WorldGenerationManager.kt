package xyz.xenondevs.nova.world.generation

import com.google.gson.JsonParser
import com.mojang.serialization.DataResult
import com.mojang.serialization.Dynamic
import com.mojang.serialization.JsonOps
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.data.BuiltinRegistries
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.transformer.Patcher
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.generation.codec.BlockStateCodecOverride
import xyz.xenondevs.nova.world.generation.codec.CodecOverride
import java.io.InputStream

private fun InputStream.parseJson() = JsonParser.parseReader(reader())

private fun <T> DataResult<T>.getOrThrow(message: String): T {
    return if (result().isPresent) result().get()
    else throw IllegalStateException(message, IllegalArgumentException(error().get().toString()))
}

internal object WorldGenerationManager : Initializable() {
    
    override val initializationStage = InitializationStage.PRE_WORLD
    override val dependsOn = setOf(Patcher, Resources)
    
    private val REGISTRY_ACCESS = minecraftServer.registryAccess()
    private val REGISTRY_OPS = RegistryOps.create(JsonOps.INSTANCE, REGISTRY_ACCESS)
    private val REGISTRIES = setOf(
        Registry.CONFIGURED_FEATURE_REGISTRY,
        Registry.PLACED_FEATURE_REGISTRY
    ).associateWithTo(Object2ObjectOpenHashMap()) { REGISTRY_ACCESS.registry(it).get() }
    private val BIOME_REGISTRY = REGISTRY_ACCESS.registry(Registry.BIOME_REGISTRY).get()
    
    private val CODEC_OVERRIDES = listOf(BlockStateCodecOverride)
    
    private val vanillaOverrides = Object2ObjectOpenHashMap<ResourceLocation, List<List<Holder<PlacedFeature>>>>()
    private val patchedBiomes = IntOpenHashSet()
    
    override fun init() {
        REGISTRIES.values.forEach { NMSUtils.unfreezeRegistry(it) }
        CODEC_OVERRIDES.forEach(CodecOverride::replace)
        
        // load addon world gen files
        
        REGISTRIES.values.forEach { NMSUtils.freezeRegistry(it) }
    }
    
    private fun registerFeatureConfiguration(inputStream: InputStream, name: NamespacedId) {
        val configuredFeature = ConfiguredFeature.CODEC
            .decode(Dynamic(REGISTRY_OPS, inputStream.parseJson()))
            .getOrThrow("Failed to parse feature configuration of $name")
        
        BuiltinRegistries.registerExact(REGISTRIES[Registry.CONFIGURED_FEATURE_REGISTRY], name.toString(":"), configuredFeature.first.value())
    }
    
    private fun registerPlacedFeature(inputStream: InputStream, name: NamespacedId) {
        val placedFeature = PlacedFeature.CODEC
            .decode(Dynamic(REGISTRY_OPS, inputStream.parseJson()))
            .getOrThrow("Failed to parse placed feature of $name")
        
        BuiltinRegistries.registerExact(REGISTRIES[Registry.PLACED_FEATURE_REGISTRY], name.toString(":"), placedFeature.first.value())
    }
    
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun injectFeatures(list: List<Any>) {
        if (list.isEmpty() || list[0] !is Holder<*> || (list[0] as Holder<Any>).value() !is Biome) return
        (list as List<Holder<Biome>>).asSequence().map { it.value() }.forEach { biome ->
            val hash = System.identityHashCode(biome)
            if (hash in patchedBiomes) return@forEach
            val biomeFeatures = biome.generationSettings.features()
            val key = BIOME_REGISTRY.getKey(biome)
            
            if (key == null) {
                LOGGER.warning("Failed to inject features into biome ${biome.generationSettings}")
                return@forEach
            }
            
            val overrides = vanillaOverrides[key] ?: return@forEach
            
            for (i in overrides.indices) {
                if (i > biomeFeatures.size) {
                    // TODO: add needed empty lists
                    LOGGER.warning("Failed to inject requested features into biome $key: Index $i is out of bounds!")
                    break
                }
                val features = biomeFeatures[i]
                val newFeatures = ArrayList(features.unwrap().right().get()).apply { addAll(overrides[i]) }
                ReflectionUtils.setFinalField(ReflectionRegistry.HOLDER_SET_DIRECT_CONTENTS_FIELD, features, newFeatures)
                ReflectionUtils.setFinalField(ReflectionRegistry.HOLDER_SET_DIRECT_CONTENTS_SET_FIELD, features, newFeatures.toSet())
                patchedBiomes.add(hash)
            }
            
            println("Patched new biome: $biome")
        }
    }
    
}