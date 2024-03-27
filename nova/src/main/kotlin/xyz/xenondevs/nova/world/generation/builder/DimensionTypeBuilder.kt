package xyz.xenondevs.nova.world.generation.builder

import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.util.valueproviders.IntProvider
import net.minecraft.util.valueproviders.UniformInt
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.dimension.DimensionType.MonsterSettings
import xyz.xenondevs.nova.addon.registry.worldgen.DimensionRegistry
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import java.util.*

/**
 * Builder for [DimensionTypes][DimensionType]. Use [build] to get the [DimensionType] instance or [register] to register
 * it. Check out the [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/dimension/) on dimensions for more information.
 *
 * @see [DimensionRegistry]
 * @see [MobSpawnSettingsBuilder]
 */
@ExperimentalWorldGen
class DimensionTypeBuilder(id: ResourceLocation) : RegistryElementBuilder<DimensionType>(VanillaRegistries.DIMENSION_TYPE, id) {
    
    private var fixedTime: Long? = null
    private var hasSkyLight: Boolean = true
    private var hasCeiling: Boolean = false
    private var ultraWarm: Boolean = false
    private var natural: Boolean = true
    private var coordinateScale: Double = 1.0
    private var bedWorks: Boolean = true
    private var respawnAnchorWorks: Boolean = false
    private var minY: Int = -64
    private var height: Int = 384
    private var logicalHeight: Int = 384
    private var infiniBurn: TagKey<Block> = BlockTags.INFINIBURN_OVERWORLD
    private var effects: ResourceLocation = BuiltinDimensionTypes.OVERWORLD_EFFECTS
    private var ambientLight: Float = 0.0f
    private var monsterSettings: MonsterSettings = MonsterSettingsBuilder().build()
    
    /**
     * Sets the `fixedTime` property of this [DimensionType]. If this is set the tick time of any level of this dimension
     * type will be fixed to the given value. To revert it back to `null` use [noFixedTime].
     */
    fun fixedTime(time: Long) {
        fixedTime = time
    }
    
    /**
     * Sets the `fixedTime` property of this [DimensionType] to `null` and thus re-enables a normal day/light cycle.
     */
    fun noFixedTime() {
        fixedTime = null
    }
    
    /**
     * Sets the `hasSkyLight` property of this [DimensionType], which determines if this dimension type has a sky light
     * source.
     */
    fun hasSkyLight(skylight: Boolean) {
        hasSkyLight = skylight
    }
    
    /**
     * Sets the `hasCeiling` property of this [DimensionType], which determines if this dimension type has a bedrock
     * ceiling (like the nether). Please note that this property won't actually lead to a bedrock ceiling being generated.
     * It's only used to handle other gameplay mechanics like mob spawning or maps.
     */
    fun hasCeiling(ceiling: Boolean) {
        hasCeiling = ceiling
    }
    
    /**
     * Sets the `ultraWarm` property of this [DimensionType], which determines if this dimension type should behave similar
     * to the nether regarding water evaporation, lava spread or stalactite fluid drops.
     */
    fun ultraWarm(ultraWarm: Boolean) {
        this.ultraWarm = ultraWarm
    }
    
    /**
     * Sets the `natural` property of this [DimensionType]. If this is set to `false`, compasses will spin randomly, beds
     * are useless regarding spawn points. If `true` they will work as expected and nether portals will spawn zombified
     * piglins.
     */
    fun natural(natural: Boolean) {
        this.natural = natural
    }
    
    /**
     * Sets the `coordinateScale` property of this [DimensionType], which determines the multiplier applied to the
     * coordinates when leaving the dimension. (e.g. 8.0 for the nether)
     */
    fun coordinateScale(scale: Double) {
        coordinateScale = scale
    }
    
    /**
     * Sets the `bedWorks` property of this [DimensionType], which will lead to beds blowing up when trying to sleep if
     * set to `false`.
     */
    fun bedWorks(bedWorks: Boolean) {
        this.bedWorks = bedWorks
    }
    
    /**
     * Sets the `respawnAnchorWorks` property of this [DimensionType], which will lead to respawn anchors blowing up when
     * trying to use them if set to `false`.
     */
    fun respawnAnchorWorks(respawnAnchorWorks: Boolean) {
        this.respawnAnchorWorks = respawnAnchorWorks
    }
    
    /**
     * Sets the `minY` property of this [DimensionType], which determines the minimum y level that can contain blocks. Must
     * be a multiple of 16 and a value between `-2032` and `2016`. The max height `(minY + height - 1)` cannot exceed `2031`.
     */
    fun minY(minY: Int) {
        this.minY = minY
    }
    
    /**
     * Sets the `height` property of this [DimensionType], which determines the amount of layers that can contain blocks.
     * Must be a multiple of 16 and a value between `0` and `4096`. The max height `(minY + height - 1)` cannot exceed `2031`.
     */
    fun height(height: Int) {
        this.height = height
    }
    
    /**
     * Sets the `logicalHeight` property of this [DimensionType], which determines the max y level a player can be teleported
     * to by chorus fruits and nether portals. Can't be higher than the `height` property.
     */
    fun logicalHeight(logicalHeight: Int) {
        this.logicalHeight = logicalHeight
    }
    
    /**
     * Sets the `infiniBurn` property of this [DimensionType], which determines the tag of blocks that will burn infinitely.
     * (For example netherrack and magma blocks in the overworld). Minecraft ships the following default tags:
     * * `BlockTags.INFINIBURN_OVERWORLD`
     * * `BlockTags.INFINIBURN_NETHER`
     * * `BlockTags.INFINIBURN_END`
     */
    fun infiniBurn(infiniBurn: TagKey<Block>) {
        this.infiniBurn = infiniBurn
    }
    
    /**
     * Sets the `effects` property of this [DimensionType], which determines the ambient effects the client will display
     * in levels of this [DimensionType]. These are hardcoded client-side so only the vanilla values can be used:
     * * BuiltinDimensionTypes.OVERWORLD_EFFECTS
     * * BuiltinDimensionTypes.NETHER_EFFECTS
     * * BuiltinDimensionTypes.END_EFFECTS
     *
     * For a deeper understanding of what these values do, check out the [Minecraft Wiki](https://minecraft.wiki/w/Custom_dimension#Syntax)
     * (effects property).
     */
    fun effects(effects: ResourceLocation) {
        this.effects = effects
    }
    
    /**
     * Sets the `ambientLight` property of this [DimensionType].
     */
    fun ambientLight(ambientLight: Float) {
        this.ambientLight = ambientLight
    }
    
    /**
     * Sets the [MonsterSettings] of this [DimensionType]. For more information check out the [MonsterSettingsBuilder]
     */
    fun monsterSettings(monsterSettings: MonsterSettings) {
        this.monsterSettings = monsterSettings
    }
    
    /**
     * Sets the [MonsterSettings] of this [DimensionType] using a [MonsterSettingsBuilder].
     */
    @WorldGenDsl
    fun monsterSettings(builder: MonsterSettingsBuilder.() -> Unit) {
        this.monsterSettings = MonsterSettingsBuilder().apply(builder).build()
    }
    
    /**
     * Builds a [DimensionType] instance from the current state of this builder.
     */
    override fun build(): DimensionType {
        return DimensionType(
            fixedTime?.let(OptionalLong::of) ?: OptionalLong.empty(),
            hasSkyLight,
            hasCeiling,
            ultraWarm,
            natural,
            coordinateScale,
            bedWorks,
            respawnAnchorWorks,
            minY,
            height,
            logicalHeight,
            infiniBurn,
            effects,
            ambientLight,
            monsterSettings
        )
    }
    
}

/**
 * A builder for [MonsterSettings]. The following settings are available:
 *
 * * [piglinSafe] - Whether piglins transform into zombified entities.
 * * [hasRaids] - Whether players with bad omen can trigger raids.
 * * [monsterSpawnLightTest] - The light level test used to determine if a monster can spawn. (Value between 0 and 15)
 * * [monsterSpawnBlockLightLimit] - The maximum light level a block can have to allow monsters to spawn. (Value between 0 and 15)
 */
@ExperimentalWorldGen
class MonsterSettingsBuilder {
    
    private var piglinSafe: Boolean = false
    private var hasRaids: Boolean = false
    private var monsterSpawnLightTest: IntProvider = UniformInt.of(0, 7)
    private var monsterSpawnBlockLightLimit: Int = 0
    
    /**
     * Sets the `piglinSafe` property which determines whether piglins transform into zombified entities.
     */
    fun piglinSafe(piglinSafe: Boolean) {
        this.piglinSafe = piglinSafe
    }
    
    /**
     * Sets the `piglinSafe` property which determines whether players with bad omen can trigger raids.
     */
    fun hasRaids(hasRaids: Boolean) {
        this.hasRaids = hasRaids
    }
    
    /**
     * Sets the `monsterSpawnLightTest` property which determines the light level test used to determine if a monster can
     * spawn. (Value between 0 and 15)
     */
    fun monsterSpawnLightTest(test: IntProvider) {
        this.monsterSpawnLightTest = test
    }
    
    /**
     * Sets the `monsterSpawnBlockLightLimit` property which determines the maximum light level a block can have to allow
     * monsters to spawn. (Value between 0 and 15)
     */
    fun monsterSpawnBlockLightLimit(limit: Int) {
        this.monsterSpawnBlockLightLimit = limit
    }
    
    /**
     * Builds a [MonsterSettings] instance from the current state of this builder.
     */
    fun build(): MonsterSettings {
        return MonsterSettings(
            piglinSafe,
            hasRaids,
            monsterSpawnLightTest,
            monsterSpawnBlockLightLimit
        )
    }
    
}