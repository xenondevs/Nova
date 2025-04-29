@file:Suppress("LeakingThis", "UnstableApiUsage")

package xyz.xenondevs.nova.addon

import com.mojang.serialization.MapCodec
import io.papermc.paper.plugin.configuration.PluginMeta
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import it.unimi.dsi.fastutil.doubles.DoubleList
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.carver.CarverConfiguration
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver
import net.minecraft.world.level.levelgen.carver.WorldCarver
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.placement.PlacementModifier
import net.minecraft.world.level.levelgen.placement.PlacementModifierType
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureSet
import net.minecraft.world.level.levelgen.structure.StructureType
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType
import net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Cat
import org.bukkit.entity.Chicken
import org.bukkit.entity.Cow
import org.bukkit.entity.Frog
import org.bukkit.entity.Pig
import org.bukkit.entity.Player
import org.bukkit.entity.Wolf
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.addon.registry.AddonRegistryHolder
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.patch.impl.registry.set
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.buildRegistryElementLater
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.equipment.AnimatedEquipmentLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.equipment.EquipmentLayout
import xyz.xenondevs.nova.resources.builder.layout.equipment.StaticEquipmentLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.gui.TooltipStyleBuilder
import xyz.xenondevs.nova.serialization.json.serializer.RecipeDeserializer
import xyz.xenondevs.nova.ui.menu.explorer.recipes.group.RecipeGroup
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider
import xyz.xenondevs.nova.update.ProjectDistributor
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaBlockBuilder
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityBlockBuilder
import xyz.xenondevs.nova.world.block.TileEntityConstructor
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.LocalValidator
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConstructor
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkGroupConstructor
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilterType
import xyz.xenondevs.nova.world.entity.CatVariantBuilder
import xyz.xenondevs.nova.world.entity.ChickenVariantBuilder
import xyz.xenondevs.nova.world.entity.CowVariantBuilder
import xyz.xenondevs.nova.world.entity.FrogVariantBuilder
import xyz.xenondevs.nova.world.entity.PigVariantBuilder
import xyz.xenondevs.nova.world.entity.WolfSoundVariantBuilder
import xyz.xenondevs.nova.world.entity.WolfVariantBuilder
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.BiomeBuilder
import xyz.xenondevs.nova.world.generation.builder.BiomeInjectionBuilder
import xyz.xenondevs.nova.world.generation.builder.DimensionTypeBuilder
import xyz.xenondevs.nova.world.generation.builder.PlacedFeatureBuilder
import xyz.xenondevs.nova.world.item.Equipment
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.NovaItemBuilder
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.world.item.enchantment.EnchantmentBuilder
import xyz.xenondevs.nova.world.item.recipe.NovaRecipe
import xyz.xenondevs.nova.world.item.recipe.RecipeType
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.player.ability.Ability
import xyz.xenondevs.nova.world.player.ability.AbilityType
import xyz.xenondevs.nova.world.player.attachment.Attachment
import xyz.xenondevs.nova.world.player.attachment.AttachmentType
import java.nio.file.Path
import kotlin.reflect.KClass

internal const val REGISTRIES_DEPRECATION = "All registration functions have been moved to the Addon class."

@PublishedApi
internal val Addon.name: String
    get() = pluginMeta.name

@PublishedApi
internal val Addon.id: String
    get() = pluginMeta.name.lowercase()

@PublishedApi
internal val Addon.version: String
    get() = pluginMeta.version

abstract class Addon : AddonGetter {
    
    final override val addon: Addon
        get() = this
    
    @Deprecated(REGISTRIES_DEPRECATION)
    val registry = AddonRegistryHolder(this)
    
    /**
     * A list of [ProjectDistributors][ProjectDistributor] that distribute this addon
     * and should be checked for updates.
     */
    open val projectDistributors: List<ProjectDistributor>
        get() = emptyList()
    
    /**
     * The [JavaPlugin] instance of this addon, null during bootstrap phase.
     */
    var plugin: JavaPlugin? = null
        internal set
    
    /**
     * The [PluginMeta] of this addon.
     */
    lateinit var pluginMeta: PluginMeta
        internal set
    
    /**
     * The [Path] of the file of this addon.
     */
    lateinit var file: Path
        internal set
    
    /**
     * The [Path] of the data folder of this addon.
     */
    lateinit var dataFolder: Path
        internal set
    
    /**
     * The [ComponentLogger] of this addon.
     */
    lateinit var logger: ComponentLogger
        internal set
    
    //<editor-fold desc="abilities">
    fun <T : Ability> registerAbilityType(name: String, abilityCreator: (Player) -> T): AbilityType<T> {
        val id = Key(addon, name)
        val abilityType = AbilityType(id, abilityCreator)
        
        NovaRegistries.ABILITY_TYPE[id] = abilityType
        return abilityType
    }
    //</editor-fold>
    
    //<editor-fold desc="attachments">
    fun <T : Attachment> registerAttachmentType(name: String, constructor: (Player) -> T): AttachmentType<T> {
        val id = Key(addon, name)
        val attachmentType = AttachmentType(id, constructor)
        
        NovaRegistries.ATTACHMENT_TYPE[id] = attachmentType
        return attachmentType
    }
    //</editor-fold>
    
    //<editor-fold desc="blocks">
    
    fun tileEntity(name: String, constructor: TileEntityConstructor, tileEntity: NovaTileEntityBlockBuilder.() -> Unit): NovaTileEntityBlock =
        NovaTileEntityBlockBuilder(addon, name, constructor).apply(tileEntity).register()
    
    fun block(name: String, block: NovaBlockBuilder.() -> Unit): NovaBlock =
        NovaBlockBuilder(addon, name).apply(block).register()
    //</editor-fold>
    
    //<editor-fold desc="enchantments">
    fun enchantment(name: String, enchantment: EnchantmentBuilder.() -> Unit): Provider<Enchantment> =
        EnchantmentBuilder(Key(addon, name)).apply(enchantment).register()
    //</editor-fold>
    
    //<editor-fold desc="entity variants">
    /**
     * Registers a new [Cat.Type] under [name] after configuring it with [catVariant].
     */
    fun catVariant(name: String, catVariant: CatVariantBuilder.() -> Unit): Provider<Cat.Type> =
        CatVariantBuilder(Key(addon, name)).apply(catVariant).register()
    
    /**
     * Registers a new [Chicken.Variant] under [name] after configuring it with [chickenVariant].
     */
    fun chickenVariant(name: String, chickenVariant: ChickenVariantBuilder.() -> Unit): Provider<Chicken.Variant> =
        ChickenVariantBuilder(Key(addon, name)).apply(chickenVariant).register()
    
    /**
     * Registers a new [Cow.Variant] under [name] after configuring it with [cowVariant].
     */
    fun cowVariant(name: String, cowVariant: CowVariantBuilder.() -> Unit): Provider<Cow.Variant> =
        CowVariantBuilder(Key(addon, name)).apply(cowVariant).register()
    
    /**
     * Registers a new [Frog.Variant] under [name] after configuring it with [cowVariant].
     */
    fun frogVariant(name: String, cowVariant: FrogVariantBuilder.() -> Unit): Provider<Frog.Variant> =
        FrogVariantBuilder(Key(addon, name)).apply(cowVariant).register()
    
    /**
     * Registers a new [Pig.Variant] under [name] after configuring it with [cowVariant].
     */
    fun pigVariant(name: String, cowVariant: PigVariantBuilder.() -> Unit): Provider<Pig.Variant> =
        PigVariantBuilder(Key(addon, name)).apply(cowVariant).register()
    
    /**
     * Registers a new [Wolf.Variant] under [name] after configuring it with [wolfVariant].
     */
    fun wolfVariant(name: String, wolfVariant: WolfVariantBuilder.() -> Unit): Provider<Wolf.Variant> =
        WolfVariantBuilder(Key(addon, name)).apply(wolfVariant).register()
    
    /**
     * Registers a new [Wolf.SoundVariant] under [name] after configuring it with [wolfSoundVariant].
     */
    fun wolfSoundVariant(name: String, wolfSoundVariant: WolfSoundVariantBuilder.() -> Unit): Provider<Wolf.SoundVariant> =
        WolfSoundVariantBuilder(Key(addon, name)).apply(wolfSoundVariant).register()
    //</editor-fold>
    
    //<editor-fold desc="equipment">
    fun equipment(name: String, layout: StaticEquipmentLayoutBuilder.() -> Unit): Equipment =
        registerEquipment(name) { StaticEquipmentLayoutBuilder(addon.id, it).apply(layout).build() }
    
    fun animatedEquipment(name: String, layout: AnimatedEquipmentLayoutBuilder.() -> Unit): Equipment =
        registerEquipment(name) { AnimatedEquipmentLayoutBuilder(addon.id, it).apply(layout).build() }
    
    private fun registerEquipment(name: String, makeLayout: (ResourcePackBuilder) -> EquipmentLayout): Equipment {
        val id = Key(addon, name)
        val equipment = Equipment(id, makeLayout)
        NovaRegistries.EQUIPMENT[id] = equipment
        return equipment
    }
    //</editor-fold>
    
    //<editor-fold desc="gui textures">
    fun guiTexture(name: String, texture: GuiTextureLayoutBuilder.() -> Unit): GuiTexture {
        val id = Key(addon, name)
        val texture = GuiTexture(id) { GuiTextureLayoutBuilder(id.namespace(), it).apply(texture).build() }
        NovaRegistries.GUI_TEXTURE[id] = texture
        return texture
    }
    //</editor-fold>
    
    //<editor-fold desc="item filter types">
    fun registerItemFilterType(name: String, itemFilterType: ItemFilterType<*>) {
        NovaRegistries.ITEM_FILTER_TYPE[ResourceLocation(addon, name)] = itemFilterType
    }
    //</editor-fold>
    
    //<editor-fold desc="items">
    fun item(name: String, item: NovaItemBuilder.() -> Unit): NovaItem =
        NovaItemBuilder(addon, name).apply(item).register()
    
    fun item(block: NovaBlock, name: String = block.id.value(), item: NovaItemBuilder.() -> Unit): NovaItem {
        require(block.id.namespace() == addon.id) { "The block must be from the same addon (${block.id})!" }
        return NovaItemBuilder.fromBlock(Key(addon, name), block).apply(item).register()
    }
    
    fun registerItem(
        name: String,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): NovaItem = item(name) {
        behaviors(*behaviors)
        localizedName?.let(::localizedName)
        hidden(isHidden)
    }
    
    fun registerItem(
        block: NovaBlock,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): NovaItem = item(block) {
        behaviors(*behaviors)
        localizedName?.let(::localizedName)
        hidden(isHidden)
    }
    
    fun registerItem(
        block: NovaBlock,
        name: String,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): NovaItem = item(block, name) {
        behaviors(*behaviors)
        localizedName?.let(::localizedName)
        hidden(isHidden)
    }
    //</editor-fold>
    
    //<editor-fold desc="network types">
    fun <T : Network<T>> registerNetworkType(
        name: String,
        createNetwork: NetworkConstructor<T>,
        createGroup: NetworkGroupConstructor<T>,
        validateLocal: LocalValidator,
        tickDelay: Provider<Int>,
        vararg holderTypes: KClass<out EndPointDataHolder>
    ): NetworkType<T> {
        val id = Key(addon, name)
        val networkType = NetworkType(
            id,
            createNetwork, createGroup, validateLocal,
            tickDelay,
            holderTypes.toHashSet()
        )
        
        NovaRegistries.NETWORK_TYPE[id] = networkType
        return networkType
    }
    //</editor-fold>
    
    //<editor-fold desc="recipe types">
    fun <T : NovaRecipe> registerRecipeType(name: String, recipeClass: KClass<T>, group: RecipeGroup<in T>, deserializer: RecipeDeserializer<T>?): RecipeType<T> {
        val id = Key(addon, name)
        val recipeType = RecipeType(id, recipeClass, group, deserializer)
        
        NovaRegistries.RECIPE_TYPE[id] = recipeType
        return recipeType
    }
    //</editor-fold>
    
    //<editor-fold desc="tool">
    fun registerToolCategory(name: String): ToolCategory {
        val id = Key(addon, name)
        val category = ToolCategory(id)
        
        NovaRegistries.TOOL_CATEGORY[id] = category
        return category
    }
    
    fun registerToolTier(name: String): ToolTier {
        val id = Key(addon, name)
        val tier = ToolTier(id, Configs["${id.namespace()}:tool_levels"].entry(id.value()))
        
        NovaRegistries.TOOL_TIER[id] = tier
        return tier
    }
    
    fun registerToolTier(name: String, level: Double): ToolTier {
        val id = Key(addon, name)
        val tier = ToolTier(id, provider(level))
        
        NovaRegistries.TOOL_TIER[id] = tier
        return tier
    }
    //</editor-fold>
    
    //<editor-fold desc="tooltip styles">
    /**
     * Registers a new [TooltipStyle] with the specified [name] and [meta].
     *
     * The tooltip textures are expected to be located under `textures/gui/sprites/tooltip/<name>_background.png` and
     * `textures/gui/sprites/tooltip/<name>_frame.png`. Their mcmeta can be configured via [meta].
     */
    fun tooltipStyle(name: String, meta: TooltipStyleBuilder.() -> Unit): TooltipStyle {
        val id = Key(addon, name)
        val style = TooltipStyle(id) { TooltipStyleBuilder(id, it).apply(meta).build() }
        NovaRegistries.TOOLTIP_STYLE[id] = style
        return style
    }
    
    // TODO: animatedTooltipStyle
    //</editor-fold>
    
    //<editor-fold desc="waila">
    fun <T> registerWailaInfoProvider(name: String, provider: WailaInfoProvider<T>): WailaInfoProvider<T> {
        val id = addon.id + ":" + name
        
        NovaRegistries.WAILA_INFO_PROVIDER[id] = provider
        return provider
    }
    
    fun registerWailaToolIconProvider(name: String, provider: WailaToolIconProvider): WailaToolIconProvider {
        val id = addon.id + ":" + name
        NovaRegistries.WAILA_TOOL_ICON_PROVIDER[id] = provider
        return provider
    }
    //</editor-fold>
    
    //<editor-fold desc="worldgen">
    fun <T : RuleTest> registerRuleTestType(name: String, ruleTestType: RuleTestType<T>): RuleTestType<T> {
        val id = ResourceLocation(addon, name)
        Registries.RULE_TEST[id] = ruleTestType
        return ruleTestType
    }
    
    fun <T : RuleTest> registerRuleTestType(name: String, codec: MapCodec<T>): RuleTestType<T> =
        registerRuleTestType(name) { codec }
    
    @ExperimentalWorldGen
    fun biomeInjection(name: String, biomeInjection: BiomeInjectionBuilder.() -> Unit) {
        buildRegistryElementLater(addon, name, Registries.BIOME, ::BiomeInjectionBuilder, biomeInjection)
    }
    
    @ExperimentalWorldGen
    fun biome(name: String, biome: BiomeBuilder.() -> Unit): ResourceKey<Biome> =
        buildRegistryElementLater(addon, name, Registries.BIOME, ::BiomeBuilder, biome)
    
    @ExperimentalWorldGen
    fun <CC : CarverConfiguration> registerCarver(name: String, carver: WorldCarver<CC>): WorldCarver<CC> {
        val id = ResourceLocation(addon, name)
        Registries.CARVER[id] = carver
        return carver
    }
    
    @ExperimentalWorldGen
    fun <CC : CarverConfiguration> registerConfiguredCarver(name: String, configuredCarver: ConfiguredWorldCarver<CC>): ConfiguredWorldCarver<CC> {
        val id = ResourceLocation(addon, name)
        Registries.CONFIGURED_CARVER[id] = configuredCarver
        return configuredCarver
    }
    
    @ExperimentalWorldGen
    fun dimensionType(name: String, dimensionType: DimensionTypeBuilder.() -> Unit): ResourceKey<DimensionType> =
        buildRegistryElementLater(addon, name, Registries.DIMENSION_TYPE, ::DimensionTypeBuilder, dimensionType)
    
    @ExperimentalWorldGen
    fun placedFeature(name: String, placedFeature: PlacedFeatureBuilder.() -> Unit): ResourceKey<PlacedFeature> =
        buildRegistryElementLater(addon, name, Registries.PLACED_FEATURE, ::PlacedFeatureBuilder, placedFeature)
    
    @ExperimentalWorldGen
    fun <FC : FeatureConfiguration, F : Feature<FC>> configuredFeature(name: String, feature: F, config: FC): ResourceKey<ConfiguredFeature<*, *>> =
        configuredFeature(name, ConfiguredFeature(feature, config))
    
    @ExperimentalWorldGen
    fun <F : ConfiguredFeature<*, *>> configuredFeature(name: String, configuredFeature: F): ResourceKey<ConfiguredFeature<*, *>> {
        val key = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation(addon, name))
        Registries.CONFIGURED_FEATURE[key] = configuredFeature
        return key
    }
    
    @ExperimentalWorldGen
    fun feature(name: String, feature: Feature<*>): ResourceKey<Feature<*>> {
        val key = ResourceKey.create(Registries.FEATURE, ResourceLocation(addon, name))
        Registries.FEATURE[key] = feature
        return key
    }
    
    @ExperimentalWorldGen
    fun <P : PlacementModifier> placementModifierType(name: String, placementModifierType: PlacementModifierType<P>): PlacementModifierType<P> {
        val id = ResourceLocation(addon, name)
        Registries.PLACEMENT_MODIFIER_TYPE[id] = placementModifierType
        return placementModifierType
    }
    
    @ExperimentalWorldGen
    fun <P : PlacementModifier> placementModifierType(name: String, codec: MapCodec<P>): PlacementModifierType<P> =
        placementModifierType(name) { codec }
    
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, noiseParams: NoiseParameters): NoiseParameters {
        val id = ResourceLocation(addon, name)
        Registries.NOISE[id] = noiseParams
        return noiseParams
    }
    
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, firstOctave: Int, amplitudes: DoubleList) =
        registerNoiseParameters(name, NoiseParameters(firstOctave, amplitudes))
    
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, firstOctave: Int, amplitudes: List<Double>) =
        registerNoiseParameters(name, NoiseParameters(firstOctave, DoubleArrayList(amplitudes)))
    
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, firstOctave: Int, vararg amplitudes: Double) =
        registerNoiseParameters(name, NoiseParameters(firstOctave, DoubleArrayList(amplitudes)))
    
    @ExperimentalWorldGen
    fun registerNoiseGenerationSettings(name: String, settings: NoiseGeneratorSettings): NoiseGeneratorSettings {
        val id = ResourceLocation(addon, name)
        Registries.NOISE_SETTINGS[id] = settings
        return settings
    }
    
    @ExperimentalWorldGen
    fun registerStructure(name: String, structure: Structure): Structure {
        val id = ResourceLocation(addon, name)
        Registries.STRUCTURE[id] = structure
        return structure
    }
    
    @ExperimentalWorldGen
    fun <P : StructurePoolElement> registerStructurePoolElementType(name: String, structurePoolElementType: StructurePoolElementType<P>): StructurePoolElementType<P> {
        val id = ResourceLocation(addon, name)
        Registries.STRUCTURE_POOL_ELEMENT[id] = structurePoolElementType
        return structurePoolElementType
    }
    
    @ExperimentalWorldGen
    fun registerStructurePieceType(name: String, structurePieceType: StructurePieceType): StructurePieceType {
        val id = ResourceLocation(addon, name)
        Registries.STRUCTURE_PIECE[id] = structurePieceType
        return structurePieceType
    }
    
    @ExperimentalWorldGen
    fun <SP : StructurePlacement> registerStructurePlacementType(name: String, structurePlacementType: StructurePlacementType<SP>): StructurePlacementType<SP> {
        val id = ResourceLocation(addon, name)
        Registries.STRUCTURE_PLACEMENT[id] = structurePlacementType
        return structurePlacementType
    }
    
    @ExperimentalWorldGen
    fun <P : StructureProcessor> registerStructureProcessorType(name: String, structureProcessorType: StructureProcessorType<P>): StructureProcessorType<P> {
        val id = ResourceLocation(addon, name)
        Registries.STRUCTURE_PROCESSOR[id] = structureProcessorType
        return structureProcessorType
    }
    
    @ExperimentalWorldGen
    fun registerStructureSet(name: String, structureSet: StructureSet): StructureSet {
        val id = ResourceLocation(addon, name)
        Registries.STRUCTURE_SET[id] = structureSet
        return structureSet
    }
    
    @ExperimentalWorldGen
    fun <S : Structure> registerStructureType(name: String, structureType: StructureType<S>): StructureType<S> {
        val id = ResourceLocation(addon, name)
        Registries.STRUCTURE_TYPE[id] = structureType
        return structureType
    }
    //</editor-fold>
    
}