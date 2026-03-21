package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import net.minecraft.advancements.criterion.MinMaxBounds
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps
import net.minecraft.world.entity.variant.BiomeCheck
import net.minecraft.world.entity.variant.MoonBrightnessCheck
import net.minecraft.world.entity.variant.PriorityProvider
import net.minecraft.world.entity.variant.SpawnCondition
import net.minecraft.world.entity.variant.SpawnContext
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import net.minecraft.world.entity.variant.StructureCheck
import org.bukkit.block.Biome
import xyz.xenondevs.nova.util.lookupGetterOrThrow
import xyz.xenondevs.nova.util.toHolderSet
import xyz.xenondevs.nova.util.toNmsTagKey

internal class SpawnConditionsBuilderImpl(
    val lookup: RegistryOps.RegistryInfoLookup
) : SpawnConditionsBuilder {
    
    private val biomeRegistry = lookup.lookupGetterOrThrow(Registries.BIOME)
    private val structureRegistry = lookup.lookupGetterOrThrow(Registries.STRUCTURE)
    
    private val conditions = ArrayList<PriorityProvider.Selector<SpawnContext, SpawnCondition>>()
    
    override fun always(priority: Int) {
        conditions += PriorityProvider.Selector(priority)
    }
    
    override fun biome(priority: Int, biome: TypedKey<Biome>, vararg biomes: TypedKey<Biome>) {
        val holderSet = listOf(biome, *biomes).toHolderSet(Registries.BIOME, biomeRegistry)
        conditions += PriorityProvider.Selector(BiomeCheck(holderSet), priority)
    }
    
    override fun biome(priority: Int, biome: TagKey<Biome>) {
        val holderSet = biomeRegistry.getOrThrow(biome.toNmsTagKey(Registries.BIOME))
        conditions += PriorityProvider.Selector(BiomeCheck(holderSet), priority)
    }
    
    override fun structure(priority: Int, structure: TypedKey<*>, vararg structures: TypedKey<*>) {
        val holderSet = listOf(structure, *structures).toHolderSet(Registries.STRUCTURE, structureRegistry)
        conditions += PriorityProvider.Selector(StructureCheck(holderSet), priority)
    }
    
    override fun structure(priority: Int, structure: TagKey<*>) {
        val holderSet = structureRegistry.getOrThrow(structure.toNmsTagKey(Registries.STRUCTURE))
        conditions += PriorityProvider.Selector(StructureCheck(holderSet), priority)
    }
    
    override fun moonBrightness(priority: Int, range: ClosedRange<Double>) {
        conditions += PriorityProvider.Selector(
            MoonBrightnessCheck(MinMaxBounds.Doubles.between(range.start, range.endInclusive)),
            priority
        )
    }
    
    fun build() = SpawnPrioritySelectors(conditions)
    
}