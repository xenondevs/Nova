package xyz.xenondevs.nova.world.generation

import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.kyori.adventure.key.Key
import net.minecraft.core.Registry
import net.minecraft.core.WritableRegistry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureSet
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.util.data.UpdatableFile
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.preFreeze
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.util.data.decodeJsonFile
import xyz.xenondevs.nova.util.data.getFirstOrThrow
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk

@OptIn(ExperimentalWorldGen::class)
@InternalInit(stage = InternalInitStage.PRE_WORLD)
internal object WorldGenFileParser {
    
    private val NOVA_WORLD_GEN_DIRECTORIES = listOf(
        NovaWorldGenDir("inject/biome", BiomeInjection.CODEC, NovaRegistries.BIOME_INJECTION, Registries.BIOME)
    )
    
    private val VANILLA_WORLD_GEN_DIRECTORIES = listOf(
        VanillaWorldGenDir("biome", Biome.DIRECT_CODEC, Registries.BIOME),
        VanillaWorldGenDir("configured_carver", ConfiguredWorldCarver.DIRECT_CODEC, Registries.CONFIGURED_CARVER),
        VanillaWorldGenDir("dimension_type", DimensionType.DIRECT_CODEC, Registries.DIMENSION_TYPE),
        VanillaWorldGenDir("configured_feature", ConfiguredFeature.DIRECT_CODEC, Registries.CONFIGURED_FEATURE),
        VanillaWorldGenDir("placed_feature", PlacedFeature.DIRECT_CODEC, Registries.PLACED_FEATURE),
        VanillaWorldGenDir("noise", NoiseParameters.DIRECT_CODEC, Registries.NOISE),
        VanillaWorldGenDir("noise_settings", NoiseGeneratorSettings.DIRECT_CODEC, Registries.NOISE_SETTINGS),
        VanillaWorldGenDir("structure", Structure.DIRECT_CODEC, Registries.STRUCTURE),
        VanillaWorldGenDir("structure_set", StructureSet.DIRECT_CODEC, Registries.STRUCTURE_SET)
    )
    
    @InitFun
    fun init() {
        UpdatableFile.extractIdNamedFromAllAddons("worldgen")
        VANILLA_WORLD_GEN_DIRECTORIES.forEach { loadFiles(it) }
        NOVA_WORLD_GEN_DIRECTORIES.forEach { loadFiles(it) }
    }
    
    private fun <T : Any> loadFiles(worldGenDir: VanillaWorldGenDir<T>) {
        worldGenDir.registry.preFreeze { registry, lookup ->
            processFiles("worldgen/${worldGenDir.dir}", worldGenDir.codec, registry, lookup)
        }
    }
    
    private fun <T : Any> loadFiles(worldGenDir: NovaWorldGenDir<T>) {
        worldGenDir.dependency.preFreeze { lookup ->
            processFiles("worldgen/${worldGenDir.dir}", worldGenDir.codec, worldGenDir.registry, lookup)
        }
    }
    
    private fun <T : Any> processFiles(
        dirName: String,
        codec: Codec<T>,
        registry: WritableRegistry<T>,
        lookup: RegistryOps.RegistryInfoLookup
    ) {
        for (addon in AddonBootstrapper.addons) {
            addon.dataFolder.resolve(dirName).walk()
                .filter { it.isRegularFile() && it.extension == "json" && ResourcePath.isValidPath(it.name) }
                .forEach { file ->
                    val id = Key.key(addon.id, file.nameWithoutExtension)
                    registry[id] = codec.decodeJsonFile(
                        RegistryOps.create(JsonOps.INSTANCE, lookup),
                        file
                    ).getFirstOrThrow("Failed to parse $file")
                }
        }
    }
    
    private data class VanillaWorldGenDir<T : Any>(
        val dir: String,
        val codec: Codec<T>,
        val registry: ResourceKey<out Registry<T>>
    )
    
    private data class NovaWorldGenDir<T : Any>(
        val dir: String,
        val codec: Codec<T>,
        val registry: WritableRegistry<T>,
        val dependency: ResourceKey<out Registry<*>>
    )
    
}