package xyz.xenondevs.nova.world.generation.builder

import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.WritableRegistry
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.placement.PlacementUtils
import net.minecraft.resources.RegistryOps.RegistryInfoLookup
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.valueproviders.ConstantInt
import net.minecraft.util.valueproviders.IntProvider
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.VerticalAnchor
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight
import net.minecraft.world.level.levelgen.placement.BiomeFilter
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter
import net.minecraft.world.level.levelgen.placement.CountOnEveryLayerPlacement
import net.minecraft.world.level.levelgen.placement.CountPlacement
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement
import net.minecraft.world.level.levelgen.placement.InSquarePlacement
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement
import net.minecraft.world.level.levelgen.placement.NoiseThresholdCountPlacement
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.placement.PlacementModifier
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement
import net.minecraft.world.level.levelgen.placement.RarityFilter
import net.minecraft.world.level.levelgen.placement.SurfaceRelativeThresholdFilter
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.registry.worldgen.FeatureRegistry
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.util.lookupGetterOrThrow
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.FeatureType

/**
 * Builder for [PlacedFeatures][PlacedFeature]. 
 * Check out the [docs page](https://xenondevs.xyz/docs-world-gen/nova/addon/worldgen/features/placed-feature/) on
 * [PlacedFeatures][PlacedFeature] for more information.
 *
 * @see [FeatureRegistry]
 * @see [PlacedFeature]
 * @see [ConfiguredFeature]
 * @see [FeatureType]
 */
@ExperimentalWorldGen
@RegistryElementBuilderDsl
class PlacedFeatureBuilder internal constructor(
    id: ResourceLocation,
    registry: WritableRegistry<PlacedFeature>,
    lookup: RegistryInfoLookup
) : RegistryElementBuilder<PlacedFeature>(registry, id) {
    
    private val configuredFeatureRegistry = lookup.lookupGetterOrThrow(Registries.CONFIGURED_FEATURE)
    
    private var configuredFeature: Holder<ConfiguredFeature<*, *>>? = null
    private val modifiers = mutableListOf<PlacementModifier>()
    
   
    /**
     * Sets the [ConfiguredFeature] that should be placed by this [PlacedFeature] to [configuredFeature].
     *
     * For more information on configured features, check out their [docs page](https://xenondevs.xyz/docs-world-gen/nova/addon/worldgen/features/features/#2-configured-feature).
     */
    fun configuredFeature(configuredFeature: ResourceKey<ConfiguredFeature<*, *>>) {
        this.configuredFeature = configuredFeatureRegistry.getOrThrow(configuredFeature)
    }
    
    /**
     * Adds a [PlacementModifier] to this [PlacedFeature].
     *
     * For more information on placement modifiers, check out their [docs page](https://xenondevs.xyz/docs-world-gen/nova/addon/worldgen/features/placed-feature/#placement-modifiers).
     */
    fun modifier(modifier: PlacementModifier) {
        modifiers += modifier
    }
    
    /**
     * Adds multiple [PlacementModifier]s to this [PlacedFeature].
     *
     * For more information on placement modifiers, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/placed-feature/#placement-modifiers).
     */
    fun modifiers(vararg modifiers: PlacementModifier) {
        this.modifiers += modifiers
    }
    
    /**
     * Adds multiple [PlacementModifier]s to this [PlacedFeature].
     *
     * For more information on placement modifiers, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/placed-feature/#placement-modifiers).
     */
    fun modifiers(modifiers: Collection<PlacementModifier>) {
        this.modifiers += modifiers
    }
    
    /**
     * Adds a [BiomeFilter] [PlacementModifier] to this [PlacedFeature], which returns the position if the configured
     * feature is registered in the biome's feature list at the given position. Empty otherwise.
     */
    fun biomeFilter() {
        modifiers += BiomeFilter.biome()
    }
    
    /**
     * Adds a [BlockPredicateFilter] [PlacementModifier] with the given [BlockPredicate] to this [PlacedFeature], which
     * returns the position if the [BlockPredicate] matches the block at the given position. Empty otherwise.
     */
    fun blockPredicateFilter(predicate: BlockPredicate) {
        modifiers += BlockPredicateFilter.forPredicate(predicate)
    }
    
    /**
     * Adds a [CountPlacement] [PlacementModifier] with the given [count] to this [PlacedFeature], which returns the
     * given position [count] times.
     */
    fun count(count: Int) {
        modifiers += CountPlacement.of(count)
    }
    
    /**
     * Adds a [CountPlacement] [PlacementModifier] with the given [count] [IntProvider] to this [PlacedFeature], which
     * returns the given position [count] times. The [IntProvider] is sampled for each position.
     */
    fun count(count: IntProvider) {
        modifiers += CountPlacement.of(count)
    }
    
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in vanilla.")
    fun countOnEveryLayer(count: Int) {
        modifiers += CountOnEveryLayerPlacement.of(count)
    }
    
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in vanilla.")
    fun countOnEveryLayer(count: IntProvider) {
        modifiers += CountOnEveryLayerPlacement.of(count)
    }
    
    /**
     * Adds an [EnvironmentScanPlacement] [PlacementModifier] to this [PlacedFeature], which scans for blocks matching
     * the given [target BlockPredicate][BlockPredicate] up/down until it finds a matching block, the
     * [max number of steps][maxSteps] is reached or the [continuePredicate] not matching the next block. If no matching
     * block is found, empty is returned.
     */
    fun environmentScan(direction: Direction, target: BlockPredicate, continuePredicate: BlockPredicate, maxSteps: Int) {
        modifiers += EnvironmentScanPlacement.scanningFor(direction, target, continuePredicate, maxSteps)
    }
    
    /**
     * Adds an [EnvironmentScanPlacement] [PlacementModifier] to this [PlacedFeature], which scans for blocks matching
     * the given [target BlockPredicate][BlockPredicate] up/down until it finds a matching block or the
     * [max number of steps][maxSteps] is reached. If no matching block is found, empty is returned.
     */
    fun environmentScan(direction: Direction, target: BlockPredicate, maxSteps: Int) {
        modifiers += EnvironmentScanPlacement.scanningFor(direction, target, maxSteps)
    }
    
    /**
     * Adds a [HeightRangePlacement] [PlacementModifier] with the given [provider] to this [PlacedFeature], which takes
     * the input position and sets the y-coordinate to a value provided by the given [HeightProvider].
     */
    fun heightRange(provider: HeightProvider) {
        modifiers += HeightRangePlacement.of(provider)
    }
    
    /**
     * Adds a [HeightRangePlacement] [PlacementModifier] with a [uniform distribution][UniformHeight] between [min] and
     * [max].
     *
     * This call is equivalent to:
     * ```kotlin
     * heightRange(UniformHeight.of(min, max))
     * ```
     */
    fun heightRangeUniform(min: VerticalAnchor, max: VerticalAnchor) {
        modifiers += HeightRangePlacement.uniform(min, max)
    }
    
    /**
     * Adds a [HeightRangePlacement] [PlacementModifier] with a [trapezoid HeightProvider][HeightProvider] and a plateau
     * of `0`.
     *
     * This call is equivalent to:
     * ```kotlin
     * heightRange(TrapezoidHeight.of(min, max))
     * ```
     */
    fun heightRangeTriangle(min: VerticalAnchor, max: VerticalAnchor) {
        modifiers += HeightRangePlacement.triangle(min, max)
    }
    
    /**
     * Adds a [HeightRangePlacement] [PlacementModifier] with a [uniform distribution][UniformHeight] between the min
     * y-level and the max terrain height.
     *
     * This call is equivalent to:
     * ```kotlin
     * heightRange(UniformHeight.of(VerticalAnchor.bottom(), VerticalAnchor.absolute(256)))
     * ```
     */
    fun inYWorldBounds() {
        modifiers += PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT
    }
    
    /**
     * Adds a [HeightmapPlacement] [PlacementModifier] with the given [heightmap] to this [PlacedFeature], which takes
     * the input position and sets the y coordinate to one block above the heightmap at the given position.
     *
     * Check out the [heightmap gist page](https://gist.github.com/ByteZ1337/31f10b0052f44acfc177f40a0f0fe9cd) for image examples.
     */
    fun heightMap(heightmap: Heightmap.Types) {
        modifiers += HeightmapPlacement.onHeightmap(heightmap)
    }
    
    /**
     * Adds a [HeightmapPlacement] [PlacementModifier] with the [Heightmap.Types.MOTION_BLOCKING] heightmap to this
     * [PlacedFeature], which takes the input position and sets the y coordinate to one block above the heightmap at the
     * given position.
     *
     * Check out the [heightmap gist page](https://gist.github.com/ByteZ1337/31f10b0052f44acfc177f40a0f0fe9cd) for image examples.
     */
    fun moveToMotionBlocking() {
        modifiers += PlacementUtils.HEIGHTMAP
    }
    
    /**
     * Adds a [HeightmapPlacement] [PlacementModifier] with the [Heightmap.Types.WORLD_SURFACE_WG] heightmap to this
     * [PlacedFeature], which takes the input position and sets the y coordinate to one block above the heightmap at the
     * given position.
     *
     * Check out the [heightmap gist page](https://gist.github.com/ByteZ1337/31f10b0052f44acfc177f40a0f0fe9cd) for image examples.
     */
    fun moveToWorldSurface() {
        modifiers += PlacementUtils.HEIGHTMAP_WORLD_SURFACE
    }
    
    /**
     * Adds a [HeightmapPlacement] [PlacementModifier] with the [Heightmap.Types.OCEAN_FLOOR] heightmap to this
     * [PlacedFeature], which takes the input position and sets the y coordinate to one block above the heightmap at the
     * given position.
     *
     * Check out the [heightmap gist page](https://gist.github.com/ByteZ1337/31f10b0052f44acfc177f40a0f0fe9cd) for image examples.
     */
    fun moveToOceanFloor() {
        modifiers += PlacementUtils.HEIGHTMAP_OCEAN_FLOOR
    }
    
    /**
     * Adds a [HeightmapPlacement] [PlacementModifier] with the [Heightmap.Types.OCEAN_FLOOR_WG] heightmap to this
     * [PlacedFeature], which takes the input position and sets the y coordinate to one block above the heightmap at the
     * given position.
     *
     * Check out the [heightmap gist page](https://gist.github.com/ByteZ1337/31f10b0052f44acfc177f40a0f0fe9cd) for image examples.
     */
    fun moveToTopSolid() {
        modifiers += PlacementUtils.HEIGHTMAP_TOP_SOLID
    }
    
    /**
     * Adds a [InSquarePlacement] [PlacementModifier] to this [PlacedFeature], which adds a random integer in the range
     * `[0; 15]` to the x- and z-coordinates of the given position.
     */
    fun inSquareSpread() {
        modifiers += InSquarePlacement.spread()
    }
    
    fun noiseBasedCount(noiseToCountRatio: Int, noiseFactor: Double, noiseOffset: Double) {
        modifiers += NoiseBasedCountPlacement.of(noiseToCountRatio, noiseFactor, noiseOffset)
    }
    
    /**
     * Adds a [NoiseThresholdCountPlacement] [PlacementModifier] with the given [noiseLevel], [belowNoise] and [aboveNoise]
     * values to this [PlacedFeature], which gets the noise value at the given position and, if the value is positive,
     * returns the given position multiple times.
     *
     * The amount of times the position is returned is determined by the following code:
     * ```java
     * double noise = Biome.BIOME_INFO_NOISE.getValue((double)pos.getX() / noiseFactor, (double)pos.getZ() / noiseFactor, false);
     * int count = (int)Math.ceil((noise + noiseOffset) * noiseToCountRatio);
     * ```
     */
    fun noiseThresholdCount(noiseLevel: Double, belowNoise: Int, aboveNoise: Int) {
        modifiers += NoiseThresholdCountPlacement.of(noiseLevel, belowNoise, aboveNoise)
    }
    
    /**
     * Adds a [RandomOffsetPlacement] [PlacementModifier] with the given [xzSpread] and [ySpread] values to this
     * [PlacedFeature], which offsets the given position by the provided [IntProvider's][IntProvider] value. Please note,
     * that the [xzSpread] [IntProvider] is sampled separately for the x- and z-coordinates.
     */
    fun randomOffset(xzSpread: IntProvider, ySpread: IntProvider) {
        modifiers += RandomOffsetPlacement.of(xzSpread, ySpread)
    }
    
    /**
     * Adds a [RandomOffsetPlacement] [PlacementModifier] with the given [xzSpread] and [ySpread] values to this
     * [PlacedFeature], which offsets the given position by the provided values.
     */
    fun randomOffset(xzSpread: Int, ySpread: Int) {
        modifiers += RandomOffsetPlacement.of(ConstantInt.of(xzSpread), ConstantInt.of(ySpread))
    }
    
    /**
     * Adds a [RandomOffsetPlacement] [PlacementModifier] with a `xzSpread` value of `0` and the given [ySpread] value
     * to this [PlacedFeature], which offsets the y-coordinate of the given position by the provided
     * [IntProvider's][IntProvider] value.
     */
    fun randomVerticalOffset(ySpread: IntProvider) {
        modifiers += RandomOffsetPlacement.of(ConstantInt.of(0), ySpread)
    }
    
    /**
     * Adds a [RandomOffsetPlacement] [PlacementModifier] with a `xzSpread` value of `0` and the given [ySpread] value to
     * this [PlacedFeature], which offsets the y-coordinate of the given position by the provided value.
     */
    fun randomVerticalOffset(ySpread: Int) {
        modifiers += RandomOffsetPlacement.of(ConstantInt.of(0), ConstantInt.of(ySpread))
    }
    
    /**
     * Adds a [RandomOffsetPlacement] [PlacementModifier] with a `ySpread` value of `0` and the given [xzSpread] value to
     * this [PlacedFeature], which offsets the x- and z-coordinates of the given position by the provided
     * [IntProvider's][IntProvider] value. Please note, that the [xzSpread] [IntProvider] is sampled separately for the
     * x- and z-coordinates.
     */
    fun randomHorizontalOffset(xzSpread: IntProvider) {
        modifiers += RandomOffsetPlacement.of(xzSpread, ConstantInt.of(0))
    }
    
    /**
     * Adds a [RandomOffsetPlacement] [PlacementModifier] with a `ySpread` value of `0` and the given [xzSpread] value to
     * this [PlacedFeature], which offsets the x- and z-coordinates of the given position by the provided value.
     */
    fun randomHorizontalOffset(xzSpread: Int) {
        modifiers += RandomOffsetPlacement.of(ConstantInt.of(xzSpread), ConstantInt.of(0))
    }
    
    /**
     * Adds a [RarityFilter] [PlacementModifier] to this [PlacedFeature], which returns the given position with a
     * probability of `1 / onAverageOnceEvery`.
     */
    fun rarityFilter(onAverageOnceEvery: Int) {
        modifiers += RarityFilter.onAverageOnceEvery(onAverageOnceEvery)
    }
    
    /**
     * Adds a [SurfaceRelativeThresholdFilter] [PlacementModifier] with the given [heightmap], [min] and [max] values to
     * this [PlacedFeature], which returns the given position if the surface height at the given position is inside the
     * specified range. Otherwise, returns empty.
     *
     * Check out the [heightmap gist page](https://gist.github.com/ByteZ1337/31f10b0052f44acfc177f40a0f0fe9cd) for image examples.
     */
    fun surfaceRelativeThresholdFilter(heightmap: Heightmap.Types, min: Int, max: Int) {
        modifiers += SurfaceRelativeThresholdFilter.of(heightmap, min, max)
    }
    
    /**
     * Adds a [SurfaceWaterDepthFilter] [PlacementModifier] with the given [maxDepth] value to this [PlacedFeature], which
     * only returns the given position if the amount of motion-blocking blocks under the surface is less than/equal to
     * [maxDepth].
     */
    fun surfaceWaterDepthFilter(maxDepth: Int) {
        modifiers += SurfaceWaterDepthFilter.forMaxDepth(maxDepth)
    }
    
    /**
     * Builds a [PlacedFeature] instance from the current state of this builder.
     */
    override fun build(): PlacedFeature {
        requireNotNull(configuredFeature) { "No configured feature was set for placed feature $id" }
        if (modifiers.isEmpty())
            LOGGER.warn("Placed feature $id has no placement modifiers!")
        
        return PlacedFeature(configuredFeature!!, modifiers)
    }
    
}