package xyz.xenondevs.nova.world.generation

import com.mojang.serialization.Codec
import net.minecraft.core.WritableRegistry
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureSet
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.DataFileParser
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.transformer.Patcher
import xyz.xenondevs.nova.util.data.decodeJsonFile
import xyz.xenondevs.nova.util.data.getFirstOrThrow
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection

@OptIn(ExperimentalWorldGen::class)
@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    dependsOn = [Patcher::class, ResourceGeneration.PreWorld::class, AddonsInitializer::class, DataFileParser::class]
)
internal object WorldGenManager {
    
    private val WORLD_GEN_DIRECTORIES = listOf(
        WorldGenDir("inject/biome", BiomeInjection.CODEC, NovaRegistries.BIOME_INJECTION),
        WorldGenDir("biome", Biome.DIRECT_CODEC, VanillaRegistries.BIOME),
        WorldGenDir("configured_carver", ConfiguredWorldCarver.DIRECT_CODEC, VanillaRegistries.CONFIGURED_CARVER),
        WorldGenDir("dimension_type", DimensionType.DIRECT_CODEC, VanillaRegistries.DIMENSION_TYPE),
        WorldGenDir("configured_feature", ConfiguredFeature.DIRECT_CODEC, VanillaRegistries.CONFIGURED_FEATURE),
        WorldGenDir("placed_feature", PlacedFeature.DIRECT_CODEC, VanillaRegistries.PLACED_FEATURE),
        WorldGenDir("noise", NoiseParameters.DIRECT_CODEC, VanillaRegistries.NOISE),
        WorldGenDir("noise_settings", NoiseGeneratorSettings.DIRECT_CODEC, VanillaRegistries.NOISE_SETTINGS),
        WorldGenDir("structure", Structure.DIRECT_CODEC, VanillaRegistries.STRUCTURE),
        WorldGenDir("structure_set", StructureSet.DIRECT_CODEC, VanillaRegistries.STRUCTURE_SET)
    )
    
    @InitFun
    fun init() {
        WORLD_GEN_DIRECTORIES.forEach { loadFiles(it) }
    }
    
    private fun <T : Any> loadFiles(worldGenDir: WorldGenDir<T>) {
        val errorName = worldGenDir.dir.replace('_', ' ')
        DataFileParser.processFiles("worldgen/${worldGenDir.dir}") { id, file ->
            val result = worldGenDir.codec
                .decodeJsonFile(file)
                .getFirstOrThrow("Failed to parse $errorName of $id at ${file.absolutePath}")
            worldGenDir.registry[id] = result
        }
    }
    
    private data class WorldGenDir<T : Any>(
        val dir: String,
        val codec: Codec<T>,
        val registry: WritableRegistry<T>
    )
    
}