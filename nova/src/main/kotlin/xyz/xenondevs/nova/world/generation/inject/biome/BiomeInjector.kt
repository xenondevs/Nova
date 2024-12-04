package xyz.xenondevs.nova.world.generation.inject.biome

import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.BiomeGenerationSettings
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.patch.impl.registry.postFreeze
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getField
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import kotlin.collections.plus

private val LOG_INJECTIONS by MAIN_CONFIG.entry<Boolean>("debug", "logging", "biome_injections")

private val BIOME_GENERATION_SETTINGS_CONSTRUCTOR = ReflectionUtils.getConstructorMethodHandle(
    BiomeGenerationSettings::class,
    HolderSet::class, List::class
)
private val BIOME_BIOME_GENERATION_SETTINGS_FIELD = getField(Biome::class, "generationSettings")
private val GENERATION_STEPS = GenerationStep.Decoration.entries.size

@OptIn(ExperimentalWorldGen::class)
@InternalInit(stage = InternalInitStage.PRE_WORLD)
internal object BiomeInjector {
    
    @InitFun
    fun prepareInjections() {
        Registries.BIOME.postFreeze { biomeRegistry, _ ->
            val toInject = HashMap<Biome, Array<MutableSet<Holder<PlacedFeature>>>>()
            for (biomeInjection in NovaRegistries.BIOME_INJECTION) {
                val biomes = biomeInjection.resolveAffectedBiomes(biomeRegistry)
                for (biome in biomes) {
                    val featuresPerStep = toInject.getOrPut(biome) { Array(GENERATION_STEPS) { HashSet() } }
                    for ((i, features) in biomeInjection.features.withIndex()) {
                        featuresPerStep[i] += features
                    }
                }
            }
            
            for ((biome, injections) in toInject) {
                val key = biomeRegistry.getKey(biome) ?: throw IllegalStateException("Biome $biome is not registered")
                if (LOG_INJECTIONS)
                    LOGGER.info("Injecting ${injections.contentToString()} into $key")
                
                injectFeatures(biome, injections.asList())
            }
        }
    }
    
    private fun injectFeatures(biome: Biome, injections: List<Set<Holder<PlacedFeature>>>) {
        val prevGenSettings: BiomeGenerationSettings = biome.generationSettings
        val prevFeatures: List<HolderSet<PlacedFeature>> = prevGenSettings.features()
        val newFeatures: List<HolderSet<PlacedFeature>> = Array<HolderSet<PlacedFeature>>(GENERATION_STEPS) { featureStep ->
            val prevFeatures = prevFeatures.getOrNull(featureStep) ?: emptyList()
            val newFeatures = injections.getOrNull(featureStep) ?: emptyList()
            HolderSet.direct(prevFeatures + newFeatures)
        }.asList()
        val newGenSettings = BIOME_GENERATION_SETTINGS_CONSTRUCTOR(prevGenSettings.carvers, newFeatures)
        
        ReflectionUtils.setFinalField(BIOME_BIOME_GENERATION_SETTINGS_FIELD, biome, newGenSettings)
    }
    
}