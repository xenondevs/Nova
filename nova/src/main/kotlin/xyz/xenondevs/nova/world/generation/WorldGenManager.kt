package xyz.xenondevs.nova.world.generation

import net.minecraft.core.registries.Registries
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.DataFileParser
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.transformer.Patcher
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.util.NMSUtils.REGISTRY_ACCESS
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjector
import xyz.xenondevs.nova.world.generation.registry.BiomeInjectionRegistry
import xyz.xenondevs.nova.world.generation.registry.BiomeRegistry
import xyz.xenondevs.nova.world.generation.registry.CarverRegistry
import xyz.xenondevs.nova.world.generation.registry.DimensionRegistry
import xyz.xenondevs.nova.world.generation.registry.FeatureRegistry
import xyz.xenondevs.nova.world.generation.registry.NoiseRegistry
import xyz.xenondevs.nova.world.generation.registry.StructureRegistry

@OptIn(ExperimentalWorldGen::class)
@InternalInit(
    stage = InitializationStage.PRE_WORLD,
    dependsOn = [Patcher::class, ResourceGeneration.PreWorld::class, AddonsInitializer::class, DataFileParser::class]
)
internal object WorldGenManager {
    
    private val WORLD_GEN_REGISTRIES by lazy {
        listOf(
            FeatureRegistry, NoiseRegistry, CarverRegistry, StructureRegistry, BiomeRegistry, BiomeInjectionRegistry,
            DimensionRegistry
        )
    }
    private val NMS_REGISTRIES  by lazy {
        WORLD_GEN_REGISTRIES.asSequence()
            .flatMap { it.neededRegistries }
            .map { REGISTRY_ACCESS.registry(it).get() }
    }
    
    @InitFun
    private fun init() {
        WORLD_GEN_REGISTRIES.forEach {
            it.registerDefaults()
            it.register()
        }
        NMSUtils.getRegistry(Registries.LEVEL_STEM).forEach { BiomeInjector.injectFeatures(it.generator.biomeSource.possibleBiomes().toList()) }
        NMS_REGISTRIES.forEach(NMSUtils::freezeRegistry)
    }
    
}