package xyz.xenondevs.nova.world.entity

import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps.RegistryInfoLookup
import net.minecraft.world.entity.variant.BiomeCheck
import net.minecraft.world.entity.variant.MoonBrightnessCheck
import net.minecraft.world.entity.variant.PriorityProvider
import net.minecraft.world.entity.variant.SpawnCondition
import net.minecraft.world.entity.variant.SpawnContext
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import net.minecraft.world.entity.variant.StructureCheck
import org.bukkit.block.Biome
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.util.lookupGetterOrThrow
import xyz.xenondevs.nova.util.toHolderSet
import xyz.xenondevs.nova.util.toNmsTagKey
import java.util.*

/**
 * Builder for [Spawn Conditions](https://minecraft.wiki/w/Mob_variant_definitions#Spawn_condition).
 */
@RegistryElementBuilderDsl
class SpawnConditionsBuilder internal constructor(lookup: RegistryInfoLookup) {
    
    private val biomeRegistry = lookup.lookupGetterOrThrow(Registries.BIOME)
    private val structureRegistry = lookup.lookupGetterOrThrow(Registries.STRUCTURE)
    
    private val conditions = ArrayList<PriorityProvider.Selector<SpawnContext, SpawnCondition>>()
    
    /**
     * Adds a spawn condition of [priority] that always returns true.
     */
    fun always(priority: Int) {
        conditions += PriorityProvider.Selector(priority)
    }
    
    /**
     * Adds a spawn condition of [priority] that tests if the spawn position is inside any of the specified biomes.
     */
    fun biome(priority: Int, biome: TypedKey<Biome>, vararg biomes: TypedKey<Biome>) {
        val holderSet = listOf(biome, *biomes).toHolderSet(Registries.BIOME, biomeRegistry)
        conditions += PriorityProvider.Selector(BiomeCheck(holderSet), priority)
    }
    
    /**
     * Adds a spawn condition of [priority] that tests if the spawn position is inside any of the specified biomes.
     */
    fun biome(priority: Int, biome: TagKey<Biome>) {
        val holderSet = biomeRegistry.getOrThrow(biome.toNmsTagKey(Registries.BIOME))
        conditions += PriorityProvider.Selector(BiomeCheck(holderSet), priority)
    }
    
    /**
     * Adds a spawn condition of [priority] that tests if the spawn position is inside any of the specified structures.
     */
    fun structure(priority: Int, structure: TypedKey<*>, vararg structures: TypedKey<*>) {
        val holderSet = listOf(structure, *structures).toHolderSet(Registries.STRUCTURE, structureRegistry)
        conditions += PriorityProvider.Selector(StructureCheck(holderSet), priority)
    }
    
    /**
     * Adds a spawn condition of [priority] that tests if the spawn position is inside any of the specified structures.
     */
    fun structure(priority: Int, structure: TagKey<*>) {
        val holderSet = structureRegistry.getOrThrow(structure.toNmsTagKey(Registries.STRUCTURE))
        conditions += PriorityProvider.Selector(StructureCheck(holderSet), priority)
    }
    
    /**
     * Adds a spawn condition of [priority] that tests if the moon brightness is in the specified range.
     */
    fun moonBrightness(priority: Int, range: ClosedRange<Double>) {
        conditions += PriorityProvider.Selector(
            MoonBrightnessCheck(MinMaxBounds.Doubles(
                Optional.of(range.start),
                Optional.of(range.endInclusive),
                Optional.of(range.start * range.start),
                Optional.of(range.endInclusive * range.endInclusive)
            )),
            priority
        )
    }
    
    internal fun build() = SpawnPrioritySelectors(conditions)
    
}