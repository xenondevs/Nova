package xyz.xenondevs.nova.world.generation

import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.kyori.adventure.key.Key
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.dimension.LevelStem
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.carver.WorldCarver
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition
import net.minecraft.world.level.levelgen.material.rule.MaterialRule
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureSet
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType
import net.minecraft.world.level.levelgen.synth.NormalNoise
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.preFreeze
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.util.data.UpdatableFile
import xyz.xenondevs.nova.util.data.decodeJsonFile
import xyz.xenondevs.nova.util.data.getFirstOrThrow
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjector
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk

@InternalInit(stage = InternalInitStage.PRE_WORLD)
internal object WorldGenFileParser {
    
    private val VANILLA_WORLD_GEN_DIRECTORIES = listOf(
        VanillaWorldGenDir("biome", Biome.DIRECT_CODEC, Registries.BIOME),
        VanillaWorldGenDir("carver", WorldCarver.DIRECT_CODEC, Registries.CARVER),
        VanillaWorldGenDir("dimension", LevelStem.CODEC, Registries.LEVEL_STEM),
        VanillaWorldGenDir("dimension_type", DimensionType.DIRECT_CODEC, Registries.DIMENSION_TYPE),
        VanillaWorldGenDir("feature", Feature.DIRECT_CODEC, Registries.FEATURE),
        VanillaWorldGenDir("material_condition", MaterialCondition.DIRECT_CODEC, Registries.MATERIAL_CONDITION),
        VanillaWorldGenDir("material_rule", MaterialRule.DIRECT_CODEC, Registries.MATERIAL_RULE),
        VanillaWorldGenDir("placed_feature", PlacedFeature.DIRECT_CODEC, Registries.PLACED_FEATURE),
        VanillaWorldGenDir("noise", NormalNoise.DIRECT_CODEC, Registries.NOISE),
        VanillaWorldGenDir("noise_settings", NoiseGeneratorSettings.DIRECT_CODEC, Registries.NOISE_SETTINGS),
        VanillaWorldGenDir("structure", Structure.DIRECT_CODEC, Registries.STRUCTURE),
        VanillaWorldGenDir("structure_set", StructureSet.DIRECT_CODEC, Registries.STRUCTURE_SET),
        VanillaWorldGenDir("template_pool", StructureTemplatePool.DIRECT_CODEC, Registries.TEMPLATE_POOL),
        VanillaWorldGenDir("processor_list", StructureProcessorType.DIRECT_CODEC, Registries.PROCESSOR_LIST)
    )
    
    @InitFun
    fun init() {
        UpdatableFile.extractIdNamedFromAllAddons("worldgen")
        VANILLA_WORLD_GEN_DIRECTORIES.forEach { loadFiles(it) }
        Registries.BIOME.preFreeze { _, lookup ->
            processFiles("worldgen/inject/biome", BiomeInjection.CODEC, lookup, BiomeInjector::add)
        }
    }
    
    private fun <T : Any> loadFiles(worldGenDir: VanillaWorldGenDir<T>) {
        worldGenDir.registry.preFreeze { registry, lookup ->
            processFiles("worldgen/${worldGenDir.dir}", worldGenDir.codec, lookup) { id, value ->
                registry[id] = value
            }
        }
    }
    
    private fun <T : Any> processFiles(
        dirName: String,
        codec: Codec<T>,
        lookup: RegistryOps.RegistryInfoLookup,
        register: (Key, T) -> Unit
    ) {
        for (addon in AddonBootstrapper.addons) {
            addon.dataFolder.resolve(dirName).walk()
                .filter { it.isRegularFile() && it.extension == "json" && ResourcePath.isValidPath(it.name) }
                .forEach { file ->
                    val id = Key.key(addon.namespace(), file.nameWithoutExtension)
                    register(id, codec.decodeJsonFile(
                        RegistryOps.create(JsonOps.INSTANCE, lookup),
                        file
                    ).getFirstOrThrow("Failed to parse $file"))
                }
        }
    }
    
    private data class VanillaWorldGenDir<T : Any>(
        val dir: String,
        val codec: Codec<T>,
        val registry: ResourceKey<out Registry<T>>
    )
    
}
