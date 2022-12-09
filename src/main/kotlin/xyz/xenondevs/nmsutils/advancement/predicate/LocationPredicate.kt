package xyz.xenondevs.nmsutils.advancement.predicate

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import org.bukkit.NamespacedKey
import org.bukkit.World
import xyz.xenondevs.nmsutils.adapter.NonNullAdapter
import xyz.xenondevs.nmsutils.adapter.impl.DoubleBoundsAdapter
import xyz.xenondevs.nmsutils.advancement.AdvancementDsl
import xyz.xenondevs.nmsutils.internal.util.resourceKey
import xyz.xenondevs.nmsutils.internal.util.resourceLocation
import net.minecraft.advancements.critereon.LocationPredicate as MojangLocationPredicate

/**
 * Contains all default features.
 */
object Feature {
    
    const val BURIED_TREASURE = "minecraft:buried_treasure"
    const val DESERT_PYRAMID = "minecraft:desert_pyramid"
    const val ENDCITY = "minecraft:endcity"
    const val FORTRESS = "minecraft:fortress"
    const val IGLOO = "minecraft:igloo"
    const val JUNGLE_PYRAMID = "minecraft:jungle_pyramid"
    const val MANSION = "minecraft:mansion"
    const val MINESHAFT = "minecraft:mineshaft"
    const val MONUMENT = "minecraft:monument"
    const val OCEAN_RUIN = "minecraft:ocean_ruin"
    const val PILLAGER_OUTPOST = "minecraft:pillager_outpost"
    const val SHIPWRECK = "minecraft:shipwreck"
    const val STRONGHOLD = "minecraft:stronghold"
    const val SWAMP_HUT = "minecraft:swamp_hut"
    const val VILLAGE = "minecraft:village"
    
}

/**
 * Contains all default biomes.
 */
object Biome {
    
    const val BADLANDS = "minecraft:badlands"
    const val BAMBOO_JUNGLE = "minecraft:bamboo_jungle"
    const val BASALT_DELTAS = "minecraft:basalt_deltas"
    const val BEACH = "minecraft:beach"
    const val BIRCH_FOREST = "minecraft:birch_forest"
    const val COLD_OCEAN = "minecraft:cold_ocean"
    const val CRIMSON_FOREST = "minecraft:crimson_forest"
    const val DARK_FOREST = "minecraft:dark_forest"
    const val DEEP_COLD_OCEAN = "minecraft:deep_cold_ocean"
    const val DEEP_FROZEN_OCEAN = "minecraft:deep_frozen_ocean"
    const val DEEP_LUKEWARM_OCEAN = "minecraft:deep_lukewarm_ocean"
    const val DEEP_OCEAN = "minecraft:deep_ocean"
    const val DESERT = "minecraft:desert"
    const val DRIPSTONE_CAVES = "minecraft:dripstone_caves"
    const val END_BARRENS = "minecraft:end_barrens"
    const val END_HIGHLANDS = "minecraft:end_highlands"
    const val END_MIDLANDS = "minecraft:end_midlands"
    const val ERODED_BADLANDS = "minecraft:eroded_badlands"
    const val FLOWER_FOREST = "minecraft:flower_forest"
    const val FOREST = "minecraft:forest"
    const val FROZEN_OCEAN = "minecraft:frozen_ocean"
    const val FROZEN_PEAKS = "minecraft:frozen_peaks"
    const val FROZEN_RIVER = "minecraft:frozen_river"
    const val GROVE = "minecraft:grove"
    const val ICE_SPIKES = "minecraft:ice_spikes"
    const val JAGGED_PEAKS = "minecraft:jagged_peaks"
    const val JUNGLE = "minecraft:jungle"
    const val LUKEWARM_OCEAN = "minecraft:lukewarm_ocean"
    const val LUSH_CAVES = "minecraft:lush_caves"
    const val MEADOW = "minecraft:meadow"
    const val MUSHROOM_FIELDS = "minecraft:mushroom_fields"
    const val NETHER_WASTES = "minecraft:nether_wastes"
    const val OCEAN = "minecraft:ocean"
    const val OLD_GROWTH_BIRCH_FOREST = "minecraft:old_growth_birch_forest"
    const val OLD_GROWTH_PINE_TAIGA = "minecraft:old_growth_pine_taiga"
    const val OLD_GROWTH_SPRUCE_TAIGA = "minecraft:old_growth_spruce_taiga"
    const val PLAINS = "minecraft:plains"
    const val RIVER = "minecraft:river"
    const val SAVANNA = "minecraft:savanna"
    const val SAVANNA_PLATEAU = "minecraft:savanna_plateau"
    const val SMALL_END_ISLANDS = "minecraft:small_end_islands"
    const val SNOWY_BEACH = "minecraft:snowy_beach"
    const val SNOWY_PLAINS = "minecraft:snowy_plains"
    const val SNOWY_SLOPES = "minecraft:snowy_slopes"
    const val SNOWY_TAIGA = "minecraft:snowy_taiga"
    const val SOUL_SAND_VALLEY = "minecraft:soul_sand_valley"
    const val SPARSE_JUNGLE = "minecraft:sparse_jungle"
    const val STONY_PEAKS = "minecraft:stony_peaks"
    const val STONY_SHORE = "minecraft:stony_shore"
    const val SUNFLOWER_PLAINS = "minecraft:sunflower_plains"
    const val SWAMP = "minecraft:swamp"
    const val TAIGA = "minecraft:taiga"
    const val THE_END = "minecraft:the_end"
    const val THE_VOID = "minecraft:the_void"
    const val WARM_OCEAN = "minecraft:warm_ocean"
    const val WARPED_FOREST = "minecraft:warped_forest"
    const val WINDSWEPT_FOREST = "minecraft:windswept_forest"
    const val WINDSWEPT_GRAVELLY_HILLS = "minecraft:windswept_gravelly_hills"
    const val WINDSWEPT_HILLS = "minecraft:windswept_hills"
    const val WINDSWEPT_SAVANNA = "minecraft:windswept_savanna"
    const val WOODED_BADLANDS = "minecraft:wooded_badlands"
    
}

class LocationPredicate(
    val x: ClosedRange<Double>?,
    val y: ClosedRange<Double>?,
    val z: ClosedRange<Double>?,
    val biome: String?,
    val feature: String?,
    val world: World?,
    val smokey: Boolean?,
    val light: LightPredicate?,
    val block: BlockPredicate?,
    val fluid: FluidPredicate?
) : Predicate {
    
    companion object : NonNullAdapter<LocationPredicate, MojangLocationPredicate>(MojangLocationPredicate.ANY) {
        
        override fun convert(value: LocationPredicate): MojangLocationPredicate {
            return MojangLocationPredicate(
                DoubleBoundsAdapter.toNMS(value.x),
                DoubleBoundsAdapter.toNMS(value.y),
                DoubleBoundsAdapter.toNMS(value.z),
                value.biome?.let { ResourceKey.create(Registries.BIOME, it.resourceLocation) },
                value.feature?.let { ResourceKey.create(Registries.STRUCTURE, it.resourceLocation) },
                value.world?.resourceKey,
                value.smokey,
                LightPredicate.toNMS(value.light),
                BlockPredicate.toNMS(value.block),
                FluidPredicate.toNMS(value.fluid)
            )
        }
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var x: ClosedRange<Double>? = null
        private var y: ClosedRange<Double>? = null
        private var z: ClosedRange<Double>? = null
        private var biome: String? = null
        private var feature: String? = null
        private var world: World? = null
        private var smokey: Boolean? = null
        private var light: LightPredicate? = null
        private var block: BlockPredicate? = null
        private var fluid: FluidPredicate? = null
        
        fun x(x: ClosedRange<Double>) {
            this.x = x
        }
        
        fun y(y: ClosedRange<Double>) {
            this.y = y
        }
        
        fun z(z: ClosedRange<Double>) {
            this.z = z
        }
        
        fun biome(biome: String) {
            this.biome = biome
        }
        
        fun biome(biome: NamespacedKey) {
            this.biome = biome.toString()
        }
        
        fun feature(feature: String) {
            this.feature = feature
        }
        
        fun feature(feature: NamespacedKey) {
            this.feature = feature.toString()
        }
        
        fun world(world: World) {
            this.world = world
        }
        
        fun smokey(smokey: Boolean) {
            this.smokey = smokey
        }
        
        fun light(light: IntRange) {
            this.light = LightPredicate(light)
        }
        
        fun block(init: BlockPredicate.Builder.() -> Unit) {
            this.block = BlockPredicate.Builder().apply(init).build()
        }
        
        fun fluid(init: FluidPredicate.Builder.() -> Unit) {
            this.fluid = FluidPredicate.Builder().apply(init).build()
        }
        
        internal fun build(): LocationPredicate {
            return LocationPredicate(x, y, z, biome, feature, world, smokey, light, block, fluid)
        }
        
    }
    
}