@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.generation.builder

import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.Music
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.biome.AmbientAdditionsSettings
import net.minecraft.world.level.biome.AmbientMoodSettings
import net.minecraft.world.level.biome.AmbientParticleSettings
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biome.ClimateSettings
import net.minecraft.world.level.biome.Biome.TemperatureModifier
import net.minecraft.world.level.biome.BiomeSpecialEffects
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier
import net.minecraft.world.level.biome.MobSpawnSettings
import net.minecraft.world.level.biome.MobSpawnSettings.MobSpawnCost
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nmsutils.particle.ParticleBuilder
import xyz.xenondevs.nova.addon.registry.worldgen.BiomeRegistry
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.getOrCreateHolder
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BIOME_CLIMATE_SETTINGS_CONSTRUCTOR
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BIOME_CONSTRUCTOR
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BIOME_GENERATION_SETTINGS_CONSTRUCTOR
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BIOME_SPECIAL_EFFECTS_CONSTRUCTOR
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.MOB_SPAWN_SETTINGS_CONSTRUCTOR
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.FeatureType
import java.awt.Color
import java.util.*

/**
 * Builder for [Biomes][Biome]. Use [build] to get the [Biome] instance or [register] to register it. Check out the
 * [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/biome/) on biomes for more information.
 *
 * @see [BiomeRegistry]
 * @see [BiomeClimateSettingsBuilder]
 * @see [BiomeSpecialEffectsBuilder]
 * @see [MobSpawnSettingsBuilder]
 * @see [FeatureType]
 */
@ExperimentalWorldGen
class BiomeBuilder(id: ResourceLocation) : RegistryElementBuilder<Biome>(VanillaRegistries.BIOME, id) {
    
    private var climateSettings: ClimateSettings? = null
    private var specialEffects: BiomeSpecialEffects? = null
    private val carvers: MutableMap<GenerationStep.Carving, MutableList<Holder<ConfiguredWorldCarver<*>>>> = enumMap()
    private val features = Array(11) { mutableListOf<Holder<PlacedFeature>>() }
    private var mobSpawnSettings: MobSpawnSettings? = null
    
    /**
     * Sets the [ClimateSettings] of this biome. These settings are mostly used for foliage color and other gameplay
     * features (e.g. Color of rabbits/foxes is white if `precipitation` is `snow`).
     *
     * Check out the [BiomeClimateSettingsBuilder] documentation for a list of all available settings.
     */
    fun climateSettings(climateSettings: ClimateSettings) {
        this.climateSettings = climateSettings
    }
    
    /**
     * Sets the [ClimateSettings] of this biome using a [BiomeClimateSettingsBuilder]. These settings are mostly used
     * for foliage color and other gameplay features (e.g. Color of rabbits/foxes is white if `precipitation` is `snow`).
     *
     * Check out the [BiomeClimateSettingsBuilder] documentation for a list of all available settings.
     */
    @WorldGenDsl
    fun climateSettings(climateSettings: BiomeClimateSettingsBuilder.() -> Unit) {
        this.climateSettings = BiomeClimateSettingsBuilder().apply(climateSettings).build()
    }
    
    /**
     * Resets the [ClimateSettings] of this biome to the default values.
     */
    fun defaultClimateSettings() {
        climateSettings = null
    }
    
    /**
     * Sets the [BiomeSpecialEffects] of this biome. These settings contain a vast amount of different effects of the biome.
     *
     * Check out the [BiomeSpecialEffectsBuilder] documentation for a list of all available settings.
     */
    fun specialEffects(specialEffects: BiomeSpecialEffects) {
        this.specialEffects = specialEffects
    }
    
    /**
     * Sets the [BiomeSpecialEffects] of this biome using a [BiomeSpecialEffectsBuilder]. These settings contain a vast
     * amount of different effects of the biome.
     *
     * Check out the [BiomeSpecialEffectsBuilder] documentation for a list of all available settings.
     */
    @WorldGenDsl
    fun specialEffects(specialEffects: BiomeSpecialEffectsBuilder.() -> Unit) {
        this.specialEffects = BiomeSpecialEffectsBuilder().apply(specialEffects).build()
    }
    
    /**
     * Resets the [BiomeSpecialEffects] of this biome to the default values.
     */
    fun defaultSpecialEffects() {
        specialEffects = null
    }
    
    //<editor-fold desc="Carvers/Features" defaultstate="collapsed">
    
    /**
     * Adds a [ConfiguredWorldCarver] to this biome. The [step] parameter specifies the step at which the carver should
     * be executed (`AIR` or `LIQUID`).
     *
     * For more information on carvers, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/carvers/carvers/).
     */
    fun carver(step: GenerationStep.Carving, configuredCarver: ConfiguredWorldCarver<*>): BiomeBuilder {
        carvers.getOrPut(step, ::ArrayList).add(Holder.direct(configuredCarver))
        return this
    }
    
    /**
     * Adds a [ConfiguredWorldCarver] to this biome via a [Holder] that either already contains the [ConfiguredWorldCarver],
     * or is set later by the [ConfiguredWorldCarver Registry][Registries.CONFIGURED_FEATURE]. The [step] parameter specifies
     * the step at which the carver should be executed (`AIR` or `LIQUID`).
     *
     * For more information on carvers, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/carvers/carvers/).
     */
    fun carver(step: GenerationStep.Carving, configuredCarver: Holder<ConfiguredWorldCarver<*>>): BiomeBuilder {
        carvers.getOrPut(step, ::ArrayList).add(configuredCarver)
        return this
    }
    
    /**
     * Adds a [ConfiguredWorldCarver] to this biome via its [ResourceLocation]. If the [ConfiguredWorldCarver] is not
     * yet registered, an empty [Holder] will be created and the [ConfiguredWorldCarver] will be set later by the
     * [ConfiguredWorldCarver Registry][Registries.CONFIGURED_FEATURE]. The [step] parameter specifies the step at which
     * the carver should be executed (`AIR` or `LIQUID`).
     *
     * For more information on carvers, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/carvers/carvers/).
     */
    fun carver(step: GenerationStep.Carving, configuredCarverId: ResourceLocation): BiomeBuilder {
        carvers.getOrPut(step, ::ArrayList).add(VanillaRegistries.CONFIGURED_CARVER.getOrCreateHolder(configuredCarverId))
        return this
    }
    
    /**
     * Adds a [ConfiguredWorldCarver] to this biome via its [ResourceKey]. If the [ConfiguredWorldCarver] is not
     * yet registered, an empty [Holder] will be created and the [ConfiguredWorldCarver] will be set later by the
     * [ConfiguredWorldCarver Registry][Registries.CONFIGURED_FEATURE]. The [step] parameter specifies the step at which
     * the carver should be executed (`AIR` or `LIQUID`).
     *
     * For more information on carvers, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/carvers/carvers/).
     */
    fun carver(step: GenerationStep.Carving, configuredCarverKey: ResourceKey<ConfiguredWorldCarver<*>>): BiomeBuilder {
        return carver(step, configuredCarverKey.location())
    }
    
    /**
     * Adds multiple [ConfiguredWorldCarvers][ConfiguredWorldCarver] to this biome. The [step] parameter specifies the step at which these carvers
     * should be executed (`AIR` or `LIQUID`).
     *
     * For more information on carvers, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/carvers/carvers/).
     */
    fun carvers(step: GenerationStep.Carving, vararg configuredCarvers: ConfiguredWorldCarver<*>): BiomeBuilder {
        carvers.getOrPut(step, ::ArrayList).addAll(configuredCarvers.map { Holder.direct(it) })
        return this
    }
    
    /**
     * Adds multiple [ConfiguredWorldCarvers][ConfiguredWorldCarver] to this biome via [Holders][Holder] that either
     * already contain a [ConfiguredWorldCarver], or are set later by the [ConfiguredWorldCarver Registry][Registries.CONFIGURED_FEATURE].
     * The [step] parameter specifies the step at which these carvers should be executed (`AIR` or `LIQUID`).
     *
     * For more information on carvers, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/carvers/carvers/).
     */
    fun carvers(step: GenerationStep.Carving, vararg configuredCarvers: Holder<ConfiguredWorldCarver<*>>): BiomeBuilder {
        carvers.getOrPut(step, ::ArrayList).addAll(configuredCarvers)
        return this
    }
    
    /**
     * Adds multiple [ConfiguredWorldCarvers][ConfiguredWorldCarver] to this biome via their [ResourceLocation]. If a
     * [ConfiguredWorldCarver] is not yet registered, an empty [Holder] will be created and the [ConfiguredWorldCarver]
     * will be set later by the [ConfiguredWorldCarver Registry][Registries.CONFIGURED_FEATURE]. The [step] parameter
     * specifies the step at which these carvers should be executed (`AIR` or `LIQUID`).
     *
     * For more information on carvers, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/carvers/carvers/).
     */
    fun carvers(step: GenerationStep.Carving, vararg configuredCarverIds: ResourceLocation): BiomeBuilder {
        carvers.getOrPut(step, ::ArrayList).addAll(configuredCarverIds.map { VanillaRegistries.CONFIGURED_CARVER.getOrCreateHolder(it) })
        return this
    }
    
    /**
     * Adds multiple [ConfiguredWorldCarvers][ConfiguredWorldCarver] to this biome via their [ResourceKey]. If a [ConfiguredWorldCarver]
     * is not yet registered, an empty [Holder] will be created and the [ConfiguredWorldCarver] will be set later by the
     * [ConfiguredWorldCarver Registry][Registries.CONFIGURED_FEATURE]. The [step] parameter specifies the step at which
     * these carvers should be executed (`AIR` or `LIQUID`).
     *
     * For more information on carvers, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/carvers/carvers/).
     */
    fun carvers(step: GenerationStep.Carving, vararg configuredCarverKeys: ResourceKey<ConfiguredWorldCarver<*>>): BiomeBuilder {
        return carvers(step, *configuredCarverKeys.map { it.location() }.toTypedArray())
    }
    
    /**
     * Removes all [ConfiguredWorldCarvers][ConfiguredWorldCarver] from this biome that are executed at the specified [step].
     */
    fun resetCarvers(step: GenerationStep.Carving): BiomeBuilder {
        carvers[step] = ArrayList()
        return this
    }
    
    /**
     * Removes all [ConfiguredWorldCarvers][ConfiguredWorldCarver] from this biome.
     */
    fun resetCarvers(): BiomeBuilder {
        carvers.clear()
        return this
    }
    
    /**
     * Adds a [PlacedFeature] to this biome. The [step] parameter specifies the step at which the feature should be
     * executed (For a list of all steps, check out the [GenerationStep.Decoration] enum).
     *
     * For more information on features, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/features/).
     */
    fun feature(step: GenerationStep.Decoration, placedFeature: PlacedFeature): BiomeBuilder {
        features[step.ordinal].add(Holder.direct(placedFeature))
        return this
    }
    
    /**
     * Adds a [PlacedFeature] to this biome via a [Holder] that either already contains a [PlacedFeature], or is set
     * later by the [PlacedFeature Registry][Registries.PLACED_FEATURE]. The [step] parameter specifies the step at which
     * the feature should be executed (For a list of all steps, check out the [GenerationStep.Decoration] enum).
     *
     * For more information on features, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/features/).
     */
    fun feature(step: GenerationStep.Decoration, placedFeature: Holder<PlacedFeature>): BiomeBuilder {
        features[step.ordinal].add(placedFeature)
        return this
    }
    
    /**
     * Adds a [PlacedFeature] to this biome via its [ResourceLocation]. If the [PlacedFeature] is not yet registered, an
     * empty [Holder] will be created and the [PlacedFeature] will be set later by the [PlacedFeature Registry][Registries.PLACED_FEATURE].
     * The [step] parameter specifies the step at which the feature should be executed (For a list of all steps, check out
     * the [GenerationStep.Decoration] enum).
     *
     * For more information on features, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/features/).
     */
    fun feature(step: GenerationStep.Decoration, placedFeature: ResourceLocation): BiomeBuilder {
        features[step.ordinal].add(VanillaRegistries.PLACED_FEATURE.getOrCreateHolder(placedFeature))
        return this
    }
    
    /**
     * Adds a [PlacedFeature] to this biome via its [ResourceKey]. If the [PlacedFeature] is not yet registered, an
     * empty [Holder] will be created and the [PlacedFeature] will be set later by the [PlacedFeature Registry][Registries.PLACED_FEATURE].
     * The [step] parameter specifies the step at which the feature should be executed (For a list of all steps, check out
     * the [GenerationStep.Decoration] enum).
     *
     * For more information on features, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/features/).
     */
    fun feature(step: GenerationStep.Decoration, featureKey: ResourceKey<PlacedFeature>): BiomeBuilder {
        return feature(step, featureKey.location())
    }
    
    /**
     * Adds multiple [PlacedFeatures][PlacedFeature] to this biome. The [step] parameter specifies the step at which
     * the features should be executed (For a list of all steps, check out the [GenerationStep.Decoration] enum).
     *
     * For more information on features, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/features/).
     */
    fun features(step: GenerationStep.Decoration, vararg placedFeatures: PlacedFeature): BiomeBuilder {
        features[step.ordinal].addAll(placedFeatures.map { Holder.direct(it) })
        return this
    }
    
    /**
     * Adds multiple [PlacedFeatures][PlacedFeature] to this biome via [Holders][Holder] that either already contain a
     * [PlacedFeature], or are set later by the [PlacedFeature Registry][Registries.PLACED_FEATURE]. The [step] parameter
     * specifies the step at which the features should be executed (For a list of all steps, check out the [GenerationStep.Decoration] enum).
     *
     * For more information on features, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/features/).
     */
    fun features(step: GenerationStep.Decoration, vararg placedFeatures: Holder<PlacedFeature>): BiomeBuilder {
        features[step.ordinal].addAll(placedFeatures)
        return this
    }
    
    /**
     * Adds multiple [PlacedFeatures][PlacedFeature] to this biome via their [ResourceLocation]. If a [PlacedFeature] is
     * not yet registered, an empty [Holder] will be created and the [PlacedFeature] will be set later by the [PlacedFeature Registry][Registries.PLACED_FEATURE].
     * The [step] parameter specifies the step at which the features should be executed (For a list of all steps, check out
     * the [GenerationStep.Decoration] enum).
     *
     * For more information on features, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/features/).
     */
    fun features(step: GenerationStep.Decoration, vararg placedFeatureIds: ResourceLocation): BiomeBuilder {
        features[step.ordinal].addAll(placedFeatureIds.map { VanillaRegistries.PLACED_FEATURE.getOrCreateHolder(it) })
        return this
    }
    
    /**
     * Adds multiple [PlacedFeatures][PlacedFeature] to this biome via their [ResourceKey]. If a [PlacedFeature] is not
     * yet registered, an empty [Holder] will be created and the [PlacedFeature] will be set later by the [PlacedFeature Registry][Registries.PLACED_FEATURE].
     * The [step] parameter specifies the step at which the features should be executed (For a list of all steps, check out
     * the [GenerationStep.Decoration] enum).
     *
     * For more information on features, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/features/).
     */
    fun features(step: GenerationStep.Decoration, vararg placedFeatureKeys: ResourceKey<PlacedFeature>): BiomeBuilder {
        placedFeatureKeys.forEach { feature(step, it.location()) }
        return this
    }
    
    /**
     * Removes all [PlacedFeatures][PlacedFeature] from this biome at the specified [step].
     */
    fun resetFeatures(step: GenerationStep.Decoration): BiomeBuilder {
        features[step.ordinal] = ArrayList()
        return this
    }
    
    /**
     * Removes all [PlacedFeatures][PlacedFeature] from this biome.
     */
    fun resetFeatures(): BiomeBuilder {
        features.forEach { it.clear() }
        return this
    }
    
    //</editor-fold>
    
    /**
     * Sets the [MobSpawnSettings] of this biome. As the name suggests, this is used to configure the spawning of mobs.
     *
     * For more information on mob spawning, check out the [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/mob-spawning/).
     *
     * TODO PRE 0.13: change link ^^^^^^^^^^
     */
    fun mobSpawnSettings(mobSpawnSettings: MobSpawnSettings) {
        this.mobSpawnSettings = mobSpawnSettings
    }
    
    /**
     * Sets the [MobSpawnSettings] of this biome using a [MobSpawnSettingsBuilder]. As the name suggests, this is used to
     * configure the spawning of mobs.
     *
     * For more information on mob spawning, check out the [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/mob-spawning/).
     *
     * TODO PRE 0.13: change link ^^^^^^^^^^
     */
    @WorldGenDsl
    fun mobSpawnSettings(mobSpawnSettings: MobSpawnSettingsBuilder.() -> Unit) {
        this.mobSpawnSettings = MobSpawnSettingsBuilder().apply(mobSpawnSettings).build()
    }
    
    /**
     * Builds a [Biome] instance from the current state of this builder.
     */
    override fun build(): Biome {
        val climateSettings = this.climateSettings ?: BiomeClimateSettingsBuilder().build()
        val specialEffects = this.specialEffects ?: BiomeSpecialEffectsBuilder().build()
        val generationSettings = BIOME_GENERATION_SETTINGS_CONSTRUCTOR.newInstance(carvers, features.map { HolderSet.direct(it) })
        val mobSpawnSettings = this.mobSpawnSettings ?: MobSpawnSettings.EMPTY
        return BIOME_CONSTRUCTOR.newInstance(climateSettings, specialEffects, generationSettings, mobSpawnSettings)
    }
    
}

/**
 * Builder for a biome's [ClimateSettings]. The following settings are available:
 *
 * * [hasPrecipitation] - This determines, whether the current biome has precipitation (e.g. rain/snow).
 * * [temperature] - The temperature of the biome. Used for multiple gameplay elements like melting ice, preventing rain or changing the color of foliage (unless overridden in [BiomeSpecialEffects])
 * * [temperatureModifier] - Adjusts the previously specified temperature before calculating the height adjusted temperature
 * * [downfall] - Controls foliage color. Any value above `0.85` leads to fire burning out faster.
 */
@ExperimentalWorldGen
class BiomeClimateSettingsBuilder {
    
    private var hasPrecipitation: Boolean = false
    private var temperature: Float = 0.5f
    private var temperatureModifier: TemperatureModifier = TemperatureModifier.NONE
    private var downfall: Float = 0.5f
    
    /**
     * Sets the `hasPrecipitation` setting of the climate settings. This determines, whether the current biome has
     * rainfall (e.g. rain/snow). Please note that the [temperature] setting also affects the precipitation of a biome.
     * Any value below `0.15` will prevent precipitation.
     */
    fun hasPrecipitation(hasPrecipitation: Boolean) {
        this.hasPrecipitation = hasPrecipitation
    }
    
    /**
     * Sets the `temperature` setting of the climate settings. This determines the temperature of the biome. Used for
     * multiple gameplay elements like melting ice, preventing rain or changing the color of foliage (unless overridden in
     * [BiomeSpecialEffects]). This setting can also affect a few [Features][FeatureType].
     */
    fun temperature(temperature: Float) {
        this.temperature = temperature
    }
    
    /**
     * Sets the `temperatureModifier` setting of the climate settings. This adjusts the previously specified temperature
     * before calculating the height adjusted temperature.
     *
     * @see TemperatureModifier
     */
    fun temperatureModifier(temperatureModifier: TemperatureModifier) {
        this.temperatureModifier = temperatureModifier
    }
    
    /**
     * Sets the `downfall` setting of the climate settings. This controls foliage color. Any value above `0.85` also leads
     * to fire burning out faster.
     */
    fun downfall(downfall: Float) {
        this.downfall = downfall
    }
    
    /**
     * Builds a [ClimateSettings] instance from the current state of this builder.
     */
    fun build(): ClimateSettings {
        return BIOME_CLIMATE_SETTINGS_CONSTRUCTOR.newInstance(hasPrecipitation, temperature, temperatureModifier, downfall)
    }
    
}

/**
 * Builder for a biome's [BiomeSpecialEffects]. The following settings are available:
 *
 * * `fogColor` - The color of the fog in this biome.
 * * `waterColor` - The color of the water in this biome.
 * * `waterFogColor` - The color of the water fog in this biome.
 * * `skyColor` - The color of the sky in this biome.
 * * `foliageColorOverride` - Overrides the foliage color of this biome. If `null`, the foliage color will be calculated
 *   based on the [ClimateSettings.temperature] and [ClimateSettings.downfall] settings of the [ClimateSettings]. Use
 *   [defaultFoliageColor] to set the override back to `null`.
 * * `grassColorOverride` - Overrides the grass color of this biome. If `null`, the grass color will be calculated
 *   based on the [ClimateSettings.temperature] and [ClimateSettings.downfall] settings of the [ClimateSettings]. Use
 *   [defaultGrassColor] to set the override back to `null`.
 * * `grassColorModifier` - Adjusts the previously specified grass color before calculating the final grass color (See
 *   [GrassColorModifier]).
 * * `ambientParticleSettings` - Ambient particles that will randomly spawn around the biome. If `null`, no ambient
 *   particles will be spawned. Use [disableAmbientParticles] to set the setting back to `null`. For more information on
 *   particles, check out NMS-Utilities [ParticleBuilder] documentation.
 * * `ambientLoopSoundEvent` - Ambient sound that will play in this biome. If `null`, no ambient sound will be played.
 *   Use [disableAmbientLoopSoundEvent] to set the setting back to `null`.
 * * `ambientMoodSettings` - Ambient mood sounds (also known as cave sounds) that will play in this biome. If you're
 *   unfamiliar with Minecraft's mood sound system, check out the [Minecraft Wiki](https://minecraft.wiki/w/Ambience#Mood_algorithm).
 *   If `null`, no ambient mood sounds will be played. Use [disableAmbientMoodSound] to set the setting back to `null`.
 * * `ambientAdditionsSettings` - The ambient additions sound is a sound that will be played each tick at the probability
 *   defined at [AmbientAdditionsSettings.tickChance]. If `null`, no ambient additions sound will be played. Use
 *   [disableAmbientAdditionsSound] to set the setting back to `null`.
 * * `backgroundMusic` - The background music that will play in this biome. If `null`, no background music will be played.
 *   Use [disableBackgroundMusic] to set the setting back to `null`.
 */
@ExperimentalWorldGen
class BiomeSpecialEffectsBuilder {
    
    private var fogColor: Int = 0xC0D8FF
    private var waterColor: Int = 0x3F76E4
    private var waterFogColor: Int = 0x50533
    private var skyColor: Int = 0x78A7FF
    private var foliageColorOverride: Int? = null
    private var grassColorOverride: Int? = null
    private var grassColorModifier: GrassColorModifier = GrassColorModifier.NONE
    private var ambientParticleSettings: AmbientParticleSettings? = null
    private var ambientLoopSoundEvent: Holder<SoundEvent>? = null
    private var ambientMoodSettings: AmbientMoodSettings? = null
    private var ambientAdditionsSettings: AmbientAdditionsSettings? = null
    private var backgroundMusic: Music? = null
    
    //<editor-fold desc="Color setters" defaultstate="collapsed">
    
    /**
     * Sets the `fogColor` setting of the biome's special effects via a [Color] instance.
     */
    fun fogColor(color: Color) {
        this.fogColor = color.rgb
    }
    
    /**
     * Sets the `fogColor` setting of the biome's special effects via an RGB value (e.g. `0xC0D8FF`).
     */
    fun fogColor(color: Int) {
        this.fogColor = color
    }
    
    /**
     * Sets the `waterColor` setting of the biome's special effects via a [Color] instance.
     */
    fun waterColor(color: Color) {
        this.waterColor = color.rgb
    }
    
    /**
     * Sets the `waterColor` setting of the biome's special effects via an RGB value (e.g. `0x3F76E4`).
     */
    fun waterColor(color: Int) {
        this.waterColor = color
    }
    
    /**
     * Sets the `waterFogColor` setting of the biome's special effects via a [Color] instance.
     */
    fun waterFogColor(color: Color) {
        this.waterFogColor = color.rgb
    }
    
    /**
     * Sets the `waterFogColor` setting of the biome's special effects via an RGB value (e.g. `0x50533`).
     */
    fun waterFogColor(color: Int) {
        this.waterFogColor = color
    }
    
    /**
     * Sets the `skyColor` setting of the biome's special effects via a [Color] instance.
     */
    fun skyColor(color: Color) {
        this.skyColor = color.rgb
    }
    
    /**
     * Sets the `skyColor` setting of the biome's special effects via an RGB value (e.g. `0x78A7FF`).
     */
    fun skyColor(color: Int) {
        this.skyColor = color
    }
    
    /**
     * Sets the `foliageColorOverride` and overrides the calculated foliage color of the biome via a [Color] instance.
     * To reset the foliage color back to the default (`null`), use [defaultFoliageColor].
     */
    fun foliageColor(color: Color) {
        this.foliageColorOverride = color.rgb
    }
    
    /**
     * Sets the `foliageColorOverride` and overrides the calculated foliage color of the biome via an RGB value (e.g.
     * `0x0E8C00`). To reset the foliage color back to the default (`null`), use [defaultFoliageColor].
     */
    fun foliageColor(color: Int) {
        this.foliageColorOverride = color
    }
    
    /**
     * Resets the `foliageColorOverride` setting back to `null`, which will cause the foliage color to be calculated
     * based on the [ClimateSettings.temperature] and [ClimateSettings.downfall] settings of the [ClimateSettings].
     */
    fun defaultFoliageColor() {
        this.foliageColorOverride = null
    }
    
    /**
     * Sets the `grassColorOverride` and overrides the calculated grass color of the biome via a [Color] instance. To
     * reset the grass color back to the default (`null`), use [defaultGrassColor].
     */
    fun grassColor(color: Color) {
        this.grassColorOverride = color.rgb
    }
    
    /**
     * Sets the `grassColorOverride` and overrides the calculated grass color of the biome via an RGB value (e.g.
     * `0x0E8C00`). To reset the grass color back to the default (`null`), use [defaultGrassColor].
     */
    fun grassColor(color: Int) {
        this.grassColorOverride = color
    }
    
    /**
     * Resets the `grassColorOverride` setting back to `null`, which will cause the grass color to be calculated
     * based on the [ClimateSettings.temperature] and [ClimateSettings.downfall] settings of the [ClimateSettings].
     */
    fun defaultGrassColor() {
        this.grassColorOverride = null
    }
    
    /**
     * Sets the `grassColorModifier` setting of the biome's special effects. This setting is used to modify the
     * calculated grass color of the biome to better fit hardcoded vanilla biomes (dark forest and swamp).
     *
     * @see [GrassColorModifier]
     */
    fun grassColorModifier(grassColorModifier: GrassColorModifier) {
        this.grassColorModifier = grassColorModifier
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Ambient particles" defaultstate="collapsed">
    
    /**
     * Sets the `ambientParticle` setting of the biome's special effects. This setting is used to add ambient particles
     * to the biome. The particles are spawned randomly in the air, and the probability of spawning each tick is
     * determined by the [AmbientParticleSettings.probability] setting. To disable ambient particles again, use
     * [disableAmbientParticles].  For more information on particles, check out the NMS-Utilities [ParticleBuilder]
     * documentation.
     */
    fun ambientParticles(ambientParticleSettings: AmbientParticleSettings) {
        this.ambientParticleSettings = ambientParticleSettings
    }
    
    /**
     * Sets the `ambientParticle` setting of the biome's special effects by using a [ParticleBuilder]. This setting is
     * used to add ambient particles to the biome. The particles are spawned randomly in the air, and the probability
     * of spawning each tick is determined by the [AmbientParticleSettings.probability] setting. To disable ambient
     * particles again, use [disableAmbientParticles]. For more information on particles, check out the NMS-Utilities
     * [ParticleBuilder] documentation.
     */
    fun <T : ParticleOptions> ambientParticles(particle: ParticleType<T>, probability: Float, builder: ParticleBuilder<T>.() -> Unit = {}) {
        this.ambientParticleSettings = AmbientParticleSettings(ParticleBuilder(particle).apply(builder).getOptions(), probability)
    }
    
    /**
     * Sets the `ambientParticle` setting of the biome's special effects back to `null` to disable ambient particles.
     */
    fun disableAmbientParticles() {
        this.ambientParticleSettings = null
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Ambient sounds" defaultstate="collapsed">
    
    /**
     * Sets the `ambientLoopSoundEvent` setting of the biome's special effects. This setting is used to add ambient
     * sounds to the biome. The sound is played randomly. To disable ambient sounds again, use [disableAmbientLoopSoundEvent].
     * To disable ambient sounds again, use [disableAmbientLoopSoundEvent].
     */
    fun ambientLoopSoundEvent(ambientLoopSoundEvent: SoundEvent) {
        this.ambientLoopSoundEvent = Holder.direct(ambientLoopSoundEvent)
    }
    
    /**
     * Sets the `ambientLoopSoundEvent` setting of the biome's special effects via a Holder that either already contains
     * the [SoundEvent], or is set later by the [SoundEvent Registry][Registries.SOUND_EVENT]. To disable ambient sounds
     * again, use [disableAmbientLoopSoundEvent].
     */
    fun ambientLoopSoundEvent(ambientLoopSoundEvent: Holder<SoundEvent>) {
        this.ambientLoopSoundEvent = ambientLoopSoundEvent
    }
    
    /**
     * Sets the `ambientLoopSoundEvent` setting of the biome's special effects via its [ResourceLocation]. If the
     * [SoundEvent] is not yet registered, an empty [Holder] will be created and the [SoundEvent] will be set later by
     * the [SoundEvent Registry][Registries.SOUND_EVENT]. To disable ambient sounds again, use [disableAmbientLoopSoundEvent].
     */
    fun ambientLoopSoundEvent(soundEventId: ResourceLocation) {
        this.ambientLoopSoundEvent = VanillaRegistries.SOUND_EVENT.getOrCreateHolder(soundEventId)
    }
    
    /**
     * Sets the `ambientLoopSoundEvent` setting of the biome's special effects via its [ResourceKey]. If the
     * [SoundEvent] is not yet registered, an empty [Holder] will be created and the [SoundEvent] will be set later by
     * the [SoundEvent Registry][Registries.SOUND_EVENT]. To disable ambient sounds again, use [disableAmbientLoopSoundEvent].
     */
    fun ambientLoopSoundEvent(soundEventKey: ResourceKey<SoundEvent>) {
        this.ambientLoopSoundEvent = VanillaRegistries.SOUND_EVENT.getOrCreateHolder(soundEventKey.location())
    }
    
    /**
     * Sets the `ambientLoopSoundEvent` setting of the biome's special effects back to `null` to disable ambient sounds.
     */
    fun disableAmbientLoopSoundEvent() {
        this.ambientLoopSoundEvent = null
    }
    
    /**
     * Sets the `ambientMoodSettings` (also known as cave sounds) setting of the biome's special effects. If you're
     * unfamiliar with Minecraft's mood sound system, check out the [Minecraft Wiki](https://minecraft.wiki/w/Ambience#Mood_algorithm).
     * To disable ambient sounds again, use [disableAmbientMoodSound].
     */
    fun ambientMoodSound(moodSettings: AmbientMoodSettings) {
        this.ambientMoodSettings = moodSettings
    }
    
    /**
     * Sets the `ambientMoodSettings` (also known as cave sounds) setting of the biome's special effects by using a
     * [AmbientMoodSoundBuilder]. If you're unfamiliar with Minecraft's mood sound system, check out the
     * [Minecraft Wiki](https://minecraft.wiki/w/Ambience#Mood_algorithm). To disable ambient sounds again,
     * use [disableAmbientMoodSound].
     */
    fun ambientMoodSound(builder: AmbientMoodSoundBuilder.() -> Unit) {
        this.ambientMoodSettings = AmbientMoodSoundBuilder().apply(builder).build()
    }
    
    /**
     * Sets the `ambientMoodSettings` (also known as cave sounds) setting of the biome's special effects back to `null`
     * to disable ambient sounds.
     */
    fun disableAmbientMoodSound() {
        this.ambientMoodSettings = null
    }
    
    /**
     * Sets the `ambientAdditionsSettings` setting of the biome's special effects. This setting is used to add ambient
     * sounds to the biome. The sound is played each tick at the probability defined at [AmbientAdditionsSettings.tickChance].
     * To disable ambient sounds again, use [disableAmbientAdditionsSound].
     */
    fun ambientAdditionsSound(additionsSettings: AmbientAdditionsSettings) {
        this.ambientAdditionsSettings = additionsSettings
    }
    
    /**
     * Sets the `ambientAdditionsSettings` setting of the biome's special effects by creating a new [AmbientAdditionsSettings]
     * instance out of the given [soundEvent] and [tickProbability]. To disable ambient sounds again, use [disableAmbientAdditionsSound].
     */
    fun ambientAdditionsSound(soundEvent: SoundEvent, tickProbability: Double) {
        this.ambientAdditionsSettings = AmbientAdditionsSettings(Holder.direct(soundEvent), tickProbability)
    }
    
    /**
     * Sets the `ambientAdditionsSettings` setting of the biome's special effects by creating a new [AmbientAdditionsSettings]
     * instance out of the given [soundEvent] [Holder] and [tickProbability]. The [Holder] can either already contain the
     * [SoundEvent], or be set later by the [SoundEvent Registry][Registries.SOUND_EVENT]. To disable ambient sounds again,
     * use [disableAmbientAdditionsSound].
     */
    fun ambientAdditionsSound(soundEvent: Holder<SoundEvent>, tickProbability: Double) {
        this.ambientAdditionsSettings = AmbientAdditionsSettings(soundEvent, tickProbability)
    }
    
    /**
     * Sets the `ambientAdditionsSettings` setting of the biome's special effects by creating a new [AmbientAdditionsSettings]
     * instance out of the given [soundEventId] [ResourceLocation] and [tickProbability]. If the [SoundEvent] is not yet registered,
     * an empty [Holder] will be created and the [SoundEvent] will be set later by the [SoundEvent Registry][Registries.SOUND_EVENT].
     * To disable ambient sounds again, use [disableAmbientAdditionsSound].
     */
    fun ambientAdditionsSound(soundEventId: ResourceLocation, tickProbability: Double) {
        this.ambientAdditionsSettings = AmbientAdditionsSettings(VanillaRegistries.SOUND_EVENT.getOrCreateHolder(soundEventId), tickProbability)
    }
    
    /**
     * Sets the `ambientAdditionsSettings` setting of the biome's special effects back to `null` to disable ambient
     * additions sounds.
     */
    fun disableAmbientAdditionsSound() {
        this.ambientAdditionsSettings = null
    }
    
    /**
     * Sets the `backgroundMusic` setting of the biome's special effects. This setting is used to add background music
     * to the biome. To disable background music again, use [disableBackgroundMusic].
     */
    fun backgroundMusic(music: Music) {
        this.backgroundMusic = music
    }
    
    /**
     * Sets the `backgroundMusic` setting of the biome's special effects by using a [MusicBuilder]. To disable background
     * music again, use [disableBackgroundMusic].
     */
    fun backgroundMusic(builder: MusicBuilder.() -> Unit) {
        this.backgroundMusic = MusicBuilder().apply(builder).build()
    }
    
    /**
     * Sets the `backgroundMusic` setting of the biome's special effects back to `null` to disable background music.
     */
    fun disableBackgroundMusic() {
        this.backgroundMusic = null
    }
    
    //</editor-fold>
    
    /**
     * Builds a [BiomeSpecialEffects] instance from the current state of this builder.
     */
    fun build(): BiomeSpecialEffects {
        return BIOME_SPECIAL_EFFECTS_CONSTRUCTOR.newInstance(
            fogColor,
            waterColor,
            waterFogColor,
            skyColor,
            Optional.ofNullable(foliageColorOverride),
            Optional.ofNullable(grassColorOverride),
            grassColorModifier,
            Optional.ofNullable(ambientParticleSettings),
            Optional.ofNullable(ambientLoopSoundEvent),
            Optional.ofNullable(ambientMoodSettings),
            Optional.ofNullable(ambientAdditionsSettings),
            Optional.ofNullable(backgroundMusic)
        )
    }
    
}

/**
 * Builder for a biome's [MobSpawnSettingsBuilder]. The following settings are available:
 *
 * * `creatureGenerationProbability` - The probability of a creature spawning in a chunk. The default value is `0.1`.
 * * `spawners` - A Map of [MobCategory] -> List<[SpawnerData]> that define the spawn conditions and properties for mobs.
 * * `mobSpawnCosts` - A Map of [EntityType] -> [MobSpawnCost] that define the spawn costs for mobs. For more information
 *   on Minecraft's spawn costs system, check out the [Minecraft Wiki](https://minecraft.wiki/w/Spawn#Spawn_costs)
 */
@ExperimentalWorldGen
class MobSpawnSettingsBuilder {
    
    private var creatureGenerationProbability = 0.1f
    private val spawners = enumMap<MobCategory, MutableList<SpawnerData>>()
    private val mobSpawnCosts = mutableMapOf<EntityType<*>, MobSpawnCost>()
    
    /**
     * Sets the `creatureGenerationProbability` setting of the biome's mob spawn settings. This setting is used to
     * define the probability of a creature spawning in a chunk.
     */
    fun creatureGenerationProbability(creatureGenerationProbability: Float) {
        this.creatureGenerationProbability = creatureGenerationProbability
    }
    
    /**
     * Adds a [SpawnerData] to the `spawners` setting of the biome's mob spawn settings.
     */
    fun addSpawn(mobCategory: MobCategory, spawnerData: SpawnerData) {
        spawners.getOrPut(mobCategory, ::ArrayList).add(spawnerData)
    }
    
    /**
     * Adds a [SpawnerData] to the `spawners` setting of the biome's mob spawn settings by creating a new [SpawnerData]
     * instance out of the given [entityType], [weight], [minGroupSize] and [maxGroupSize].
     */
    fun addSpawn(mobCategory: MobCategory, entityType: EntityType<*>, weight: Int, minGroupSize: Int, maxGroupSize: Int) =
        addSpawn(mobCategory, SpawnerData(entityType, weight, minGroupSize, maxGroupSize))
    
    /**
     * Adds a [SpawnerData] to the `spawners` setting of the biome's mob spawn settings by creating a new [SpawnerData]
     * instance out of the given [entityType], [minGroupSize] and [maxGroupSize]. The `weight` is set to `1`.
     */
    fun addSpawn(mobCategory: MobCategory, entityType: EntityType<*>, minGroupSize: Int = 2, maxGroupSize: Int = 4) =
        addSpawn(mobCategory, SpawnerData(entityType, 1, minGroupSize, maxGroupSize))
    
    /**
     * Sets the spawn cost for [entityType] to the given [spawnCost].
     */
    fun setSpawnCost(entityType: EntityType<*>, spawnCost: MobSpawnCost) {
        mobSpawnCosts[entityType] = spawnCost
    }
    
    /**
     * Sets the spawn cost for [entityType] to the given [energyBudget] and [charge].
     */
    fun setSpawnCost(entityType: EntityType<*>, energyBudget: Double, charge: Double = 1.0) {
        mobSpawnCosts[entityType] = MobSpawnCost(energyBudget, charge)
    }
    
    /**
     * Builds a [MobSpawnSettings] instance from the current state of this builder.
     */
    fun build(): MobSpawnSettings {
        return MOB_SPAWN_SETTINGS_CONSTRUCTOR.newInstance(
            creatureGenerationProbability,
            spawners,
            mobSpawnCosts
        )
    }
    
}

/**
 * Builder for [AmbientMoodSoundBuilder]. If you're unfamiliar with Minecraft's mood sound system, check out the
 * [Minecraft Wiki](https://minecraft.wiki/w/Ambience#Mood_algorithm).
 *
 * The following settings are available:
 *
 * * `soundEvent` - The [SoundEvent] that is played.
 * * `tickDelay` - The delay between each sound event in ticks.
 * * `blockSearchExtent` - The range of possible positions to find place to play the mood sound.
 * * `soundPositionOffset` - The offset of the sound position.
 */
@ExperimentalWorldGen
class AmbientMoodSoundBuilder {
    
    private lateinit var soundEvent: Holder<SoundEvent>
    private var tickDelay: Int = 6000
    private var blockSearchExtent: Int = 8
    private var soundPositionOffset: Double = 2.0
    
    /**
     * Sets the `soundEvent` setting of the biome's ambient mood sound to the given [soundEvent].
     */
    fun soundEvent(soundEvent: SoundEvent) {
        this.soundEvent = Holder.direct(soundEvent)
    }
    
    /**
     * Sets the `soundEvent` setting of the biome's ambient mood sound to the given [soundEvent] via a [Holder] that
     * either already contains a [SoundEvent], or is set later by the [SoundEvent Registry][Registries.SOUND_EVENT].
     */
    fun soundEvent(soundEvent: Holder<SoundEvent>) {
        this.soundEvent = soundEvent
    }
    
    /**
     * Sets the `soundEvent` setting of the biome's ambient mood sound to the given [soundEventId] via its [ResourceLocation].
     * If the [SoundEvent] is not yet registered, an empty [Holder] will be created and the [SoundEvent] will be set later by
     * the [SoundEvent Registry][Registries.SOUND_EVENT].
     */
    fun soundEvent(soundEventId: ResourceLocation) {
        this.soundEvent = VanillaRegistries.SOUND_EVENT.getOrCreateHolder(soundEventId)
    }
    
    /**
     * Sets the `soundEvent` setting of the biome's ambient mood sound to the given [soundEventId] via its [ResourceKey].
     * If the [SoundEvent] is not yet registered, an empty [Holder] will be created and the [SoundEvent] will be set later by
     * the [SoundEvent Registry][Registries.SOUND_EVENT].
     */
    fun soundEvent(soundEventId: ResourceKey<SoundEvent>) {
        this.soundEvent = VanillaRegistries.SOUND_EVENT.getOrCreateHolder(soundEventId.location())
    }
    
    /**
     * Sets the `tickDelay` setting of the biome's ambient mood sound to the given [tickDelay].
     */
    fun tickDelay(tickDelay: Int) {
        this.tickDelay = tickDelay
    }
    
    /**
     * Sets the `blockSearchExtent` setting of the biome's ambient mood sound to the given [blockSearchExtent].
     */
    fun blockSearchExtent(blockSearchExtent: Int) {
        this.blockSearchExtent = blockSearchExtent
    }
    
    /**
     * Sets the `soundPositionOffset` setting of the biome's ambient mood sound to the given [soundPositionOffset].
     */
    fun soundPositionOffset(soundPositionOffset: Double) {
        this.soundPositionOffset = soundPositionOffset
    }
    
    /**
     * Builds a [AmbientMoodSettings] instance from the current state of this builder.
     */
    fun build() = AmbientMoodSettings(soundEvent, tickDelay, blockSearchExtent, soundPositionOffset)
    
}

/**
 * Builder for [Music].
 */
@ExperimentalWorldGen
class MusicBuilder {
    
    private lateinit var soundEvent: Holder<SoundEvent>
    private var minDelay: Int = 12000
    private var maxDelay: Int = 24000
    private var replaceCurrentMusic: Boolean = true
    
    /**
     * Sets the `soundEvent` setting of the music to the given [soundEvent].
     */
    fun soundEvent(soundEvent: SoundEvent) {
        this.soundEvent = Holder.direct(soundEvent)
    }
    
    /**
     * Sets the `soundEvent` setting of the music to the given [soundEvent] via a [Holder] that
     * either already contains a [SoundEvent], or is set later by the [SoundEvent Registry][Registries.SOUND_EVENT].
     */
    fun soundEvent(soundEvent: Holder<SoundEvent>) {
        this.soundEvent = soundEvent
    }
    
    /**
     * Sets the `soundEvent` setting of the music to the given [soundEventId] via its [ResourceLocation].
     * If the [SoundEvent] is not yet registered, an empty [Holder] will be created and the [SoundEvent] will be set later by
     * the [SoundEvent Registry][Registries.SOUND_EVENT].
     */
    fun soundEvent(soundEventId: ResourceLocation) {
        this.soundEvent = VanillaRegistries.SOUND_EVENT.getOrCreateHolder(soundEventId)
    }
    
    /**
     * Sets the `soundEvent` setting of the music to the given [soundEventId] via its [ResourceKey].
     * If the [SoundEvent] is not yet registered, an empty [Holder] will be created and the [SoundEvent] will be set later by
     * the [SoundEvent Registry][Registries.SOUND_EVENT].
     */
    fun soundEvent(soundEventId: ResourceKey<SoundEvent>) {
        this.soundEvent = VanillaRegistries.SOUND_EVENT.getOrCreateHolder(soundEventId.location())
    }
    
    /**
     * Sets the `minDelay` setting of the music to the given [minDelay].
     */
    fun minDelay(minDelay: Int) {
        this.minDelay = minDelay
    }
    
    /**
     * Sets the `maxDelay` setting of the music to the given [maxDelay].
     */
    fun maxDelay(maxDelay: Int) {
        this.maxDelay = maxDelay
    }
    
    /**
     * Sets the `replaceCurrentMusic` setting of the music to the given [replaceCurrentMusic].
     */
    fun replaceCurrentMusic(replaceCurrentMusic: Boolean) {
        this.replaceCurrentMusic = replaceCurrentMusic
    }
    
    /**
     * Builds a [Music] instance from the current state of this builder.
     */
    fun build() = Music(soundEvent, minDelay, maxDelay, replaceCurrentMusic)
    
}