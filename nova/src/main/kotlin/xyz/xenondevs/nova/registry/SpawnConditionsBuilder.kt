package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.block.Biome

/**
 * Builder for [Spawn Conditions](https://minecraft.wiki/w/Mob_variant_definitions#Spawn_condition).
 */
@RegistryElementBuilderDsl
sealed interface SpawnConditionsBuilder {
    
    /**
     * Adds a spawn condition of [priority] that always returns true.
     */
    fun always(priority: Int)
    
    /**
     * Adds a spawn condition of [priority] that tests if the spawn position is inside any of the specified biomes.
     */
    fun biome(priority: Int, biome: TypedKey<Biome>, vararg biomes: TypedKey<Biome>)
    
    /**
     * Adds a spawn condition of [priority] that tests if the spawn position is inside any of the specified biomes.
     */
    fun biome(priority: Int, biome: TagKey<Biome>)
    
    /**
     * Adds a spawn condition of [priority] that tests if the spawn position is inside any of the specified structures.
     */
    fun structure(priority: Int, structure: TypedKey<*>, vararg structures: TypedKey<*>)
    
    /**
     * Adds a spawn condition of [priority] that tests if the spawn position is inside any of the specified structures.
     */
    fun structure(priority: Int, structure: TagKey<*>)
    
    /**
     * Adds a spawn condition of [priority] that tests if the moon brightness is in the specified range.
     */
    fun moonBrightness(priority: Int, range: ClosedRange<Double>)
    
}