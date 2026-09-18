package xyz.xenondevs.nova.registry

import net.minecraft.core.HolderSet
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey
import net.minecraft.world.attribute.EnvironmentAttributeMap
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.BiomeGenerationSettings
import net.minecraft.world.level.biome.BiomeSpecialEffects
import net.minecraft.world.level.biome.MobSpawnSettings
import net.minecraft.world.level.levelgen.structure.Structure

/**
 * Configures [BiomeGenerationSettings] via [configure] and applies them to the current [Biome.BiomeBuilder].
 */
context(ctx: RegistryLookupContext)
fun Biome.BiomeBuilder.generationSettings(
    configure: BiomeGenerationSettings.Builder.() -> Unit
): Biome.BiomeBuilder {
    val settings = BiomeGenerationSettings.Builder(
        Registries.PLACED_FEATURE.holderGetter(),
        Registries.CARVER.holderGetter()
    ).apply(configure).build()
    return generationSettings(settings)
}

/**
 * Configures [BiomeSpecialEffects] via [configure] and applies them to the current [Biome.BiomeBuilder].
 */
fun Biome.BiomeBuilder.specialEffects(configure: BiomeSpecialEffects.Builder.() -> Unit): Biome.BiomeBuilder =
    specialEffects(BiomeSpecialEffects.Builder().apply(configure).build())

/**
 * Configures [MobSpawnSettings] via [configure] and applies them to the current [Biome.BiomeBuilder].
 */
fun Biome.BiomeBuilder.mobSpawnSettings(configure: MobSpawnSettings.Builder.() -> Unit): Biome.BiomeBuilder =
    mobSpawnSettings(MobSpawnSettings.Builder().apply(configure).build())

/**
 * Configures [EnvironmentAttributeMap] via [configure] and applies them to the current [Biome.BiomeBuilder].
 */
fun Biome.BiomeBuilder.attributes(configure: EnvironmentAttributeMap.Builder.() -> Unit): Biome.BiomeBuilder =
    putAttributes(EnvironmentAttributeMap.builder().apply(configure))

/**
 * Creates [Structure.StructureSettings] for [biomes] via [configure].
 */
context(ctx: RegistryLookupContext)
fun StructureSettings(
    vararg biomes: ResourceKey<Biome>,
    configure: Structure.StructureSettings.Builder.() -> Unit
): Structure.StructureSettings = Structure.StructureSettings.Builder(
    HolderSet.direct(biomes.map { it.holder() })
).apply(configure).build()

/**
 * Creates [Structure.StructureSettings] for [biomes] via [configure].
 */
context(ctx: RegistryLookupContext)
fun StructureSettings(
    biomes: TagKey<Biome>,
    configure: Structure.StructureSettings.Builder.() -> Unit
): Structure.StructureSettings = Structure.StructureSettings.Builder(
    Registries.BIOME.holderGetter().getOrThrow(biomes)
).apply(configure).build()