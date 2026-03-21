package xyz.xenondevs.nova.registry

import com.mojang.serialization.MapCodec
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import it.unimi.dsi.fastutil.doubles.DoubleList
import net.kyori.adventure.key.Key.key
import net.kyori.adventure.key.Namespaced
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
import org.bukkit.Keyed
import org.bukkit.block.BlockType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Cat
import org.bukkit.entity.Chicken
import org.bukkit.entity.Cow
import org.bukkit.entity.Frog
import org.bukkit.entity.Pig
import org.bukkit.entity.Player
import org.bukkit.entity.Wolf
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.BOOTSTRAP_LIFECYCLE
import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.equipment.AnimatedEquipmentLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.equipment.EquipmentLayout
import xyz.xenondevs.nova.resources.builder.layout.equipment.StaticEquipmentLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.gui.TooltipStyleLayoutBuilder
import xyz.xenondevs.nova.resources.builder.task.EquipmentTask
import xyz.xenondevs.nova.resources.builder.task.TooltipStyleTask
import xyz.xenondevs.nova.serialization.json.serializer.RecipeDeserializer
import xyz.xenondevs.nova.ui.menu.explorer.recipes.group.RecipeGroup
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider
import xyz.xenondevs.nova.util.Identifier
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.TileEntityConstructor
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.LocalValidator
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConstructor
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkGroupConstructor
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilterSerializer
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilterType
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.BiomeBuilder
import xyz.xenondevs.nova.world.generation.builder.BiomeInjectionBuilder
import xyz.xenondevs.nova.world.generation.builder.DimensionTypeBuilder
import xyz.xenondevs.nova.world.generation.builder.PlacedFeatureBuilder
import xyz.xenondevs.nova.world.item.Equipment
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.world.item.recipe.NovaRecipe
import xyz.xenondevs.nova.world.item.recipe.RecipeType
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.player.ability.Ability
import xyz.xenondevs.nova.world.player.ability.AbilityType
import xyz.xenondevs.nova.world.player.attachment.Attachment
import xyz.xenondevs.nova.world.player.attachment.AttachmentType
import kotlin.reflect.KClass

/**
 * For registering things under the "minecraft" namespace.
 */
internal object MinecraftRegistrar : Registrar() {
    override fun namespace() = "minecraft"
}

/**
 * For registering things under the "nova" namespace.
 */
internal object NovaRegistrar : Registrar() {
    override fun namespace() = "nova"
}

/**
 * Provides access to registration functions for things in registries.
 */
abstract class Registrar internal constructor() : Namespaced {

    fun <T : NovaRegistryElement<T>> registry(name: String, reloadable: Boolean = NovaRegistries.RELOADABLE): MutableNovaRegistry<T> =
        NovaRegistries.createRegistry(key(this, name), reloadable)
    
    fun <T : Keyed> tag(name: String, registry: RegistryKey<T>, configure: TagBuilder.Paper<T>.() -> Unit): RegistryEntrySet.Paper.Tag<T> =
        tag(registryEntrySetOf(TagKey.create(registry, name)), configure)
    
    fun <T : Keyed> tag(tag: RegistryEntrySet.Paper.Tag<T>, configure: TagBuilder.Paper<T>.() -> Unit): RegistryEntrySet.Paper.Tag<T> {
        BOOTSTRAP_LIFECYCLE.modifyTag(tag.tagKey, modify = configure)
        return tag
    }
    
    //<editor-fold desc="abilities">
    fun <T : Ability> registerAbilityType(name: String, abilityCreator: (Player) -> T): RegistryEntry.Nova<AbilityType<T>> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_ABILITY_TYPE, key(this, name)) { AbilityType(it, abilityCreator) }
    
    fun abilityTypeTag(name: String, configure: TagBuilder.Nova<AbilityType<*>>.() -> Unit): RegistryEntrySet.Nova.Tag<AbilityType<*>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_ABILITY_TYPE, key(this, name), configure)
    
    fun abilityTypeTag(tag: RegistryEntrySet.Nova.Tag<AbilityType<*>>, configure: TagBuilder.Nova<AbilityType<*>>.() -> Unit): RegistryEntrySet.Nova.Tag<AbilityType<*>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_ABILITY_TYPE, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="attachments">
    fun <T : Attachment> registerAttachmentType(name: String, constructor: (Player) -> T): RegistryEntry.Nova<AttachmentType<T>> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_ATTACHMENT_TYPE, key(this, name)) { AttachmentType(it, constructor) }
    
    fun attachmentTypeTag(name: String, configure: TagBuilder.Nova<AttachmentType<*>>.() -> Unit): RegistryEntrySet.Nova.Tag<AttachmentType<*>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_ATTACHMENT_TYPE, key(this, name), configure)
    
    fun attachmentTypeTag(tag: RegistryEntrySet.Nova.Tag<AttachmentType<*>>, configure: TagBuilder.Nova<AttachmentType<*>>.() -> Unit): RegistryEntrySet.Nova.Tag<AttachmentType<*>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_ATTACHMENT_TYPE, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="blocks">
    fun tileEntity(name: String, constructor: TileEntityConstructor, tileEntity: NovaTileEntityBlockBuilder.() -> Unit): RegistryEntry.Nova<NovaBlock> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_BLOCK, key(this, name), { NovaTileEntityBlockBuilderImpl(it, constructor) }, tileEntity)
    
    fun block(name: String, block: NovaBlockBuilder.() -> Unit): RegistryEntry.Nova<NovaBlock> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_BLOCK, key(this, name), ::NovaBlockBuilderImpl, block)
    
    fun blockTag(name: String, configure: TagBuilder.Nova<NovaBlock>.() -> Unit): RegistryEntrySet.Nova.Tag<NovaBlock> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_BLOCK, key(this, name), configure)
    
    fun blockTag(tag: RegistryEntrySet.Nova.Tag<NovaBlock>, configure: TagBuilder.Nova<NovaBlock>.() -> Unit): RegistryEntrySet.Nova.Tag<NovaBlock> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_BLOCK, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="enchantments">
    fun enchantment(name: String, enchantment: EnchantmentBuilder.() -> Unit): RegistryEntry.Paper<Enchantment> =
        RegistryLoader.enqueueVanilla(RegistryKey.ENCHANTMENT, key(this, name), ::EnchantmentBuilderImpl, enchantment)
    //</editor-fold>
    
    //<editor-fold desc="entity variants">
    /**
     * Registers a new [Cat.Type] under [name] after configuring it with [catVariant].
     */
    fun catVariant(name: String, catVariant: CatVariantBuilder.() -> Unit): RegistryEntry.Paper<Cat.Type> =
        RegistryLoader.enqueueVanilla(RegistryKey.CAT_VARIANT, key(this, name), ::CatVariantBuilderImpl, catVariant)
    
    /**
     * Registers a new [Chicken.Variant] under [name] after configuring it with [chickenVariant].
     */
    fun chickenVariant(name: String, chickenVariant: ChickenVariantBuilder.() -> Unit): RegistryEntry.Paper<Chicken.Variant> =
        RegistryLoader.enqueueVanilla(RegistryKey.CHICKEN_VARIANT, key(this, name), ::ChickenVariantBuilderImpl, chickenVariant)
    
    /**
     * Registers a new [Cow.Variant] under [name] after configuring it with [cowVariant].
     */
    fun cowVariant(name: String, cowVariant: CowVariantBuilder.() -> Unit): RegistryEntry.Paper<Cow.Variant> =
        RegistryLoader.enqueueVanilla(RegistryKey.COW_VARIANT, key(this, name), ::CowVariantBuilderImpl, cowVariant)
    
    /**
     * Registers a new [Frog.Variant] under [name] after configuring it with [frogVariant].
     */
    fun frogVariant(name: String, frogVariant: FrogVariantBuilder.() -> Unit): RegistryEntry.Paper<Frog.Variant> =
        RegistryLoader.enqueueVanilla(RegistryKey.FROG_VARIANT, key(this, name), ::FrogVariantBuilderImpl, frogVariant)
    
    /**
     * Registers a new [Pig.Variant] under [name] after configuring it with [pigVariant].
     */
    fun pigVariant(name: String, pigVariant: PigVariantBuilder.() -> Unit): RegistryEntry.Paper<Pig.Variant> =
        RegistryLoader.enqueueVanilla(RegistryKey.PIG_VARIANT, key(this, name), ::PigVariantBuilderImpl, pigVariant)
    
    /**
     * Registers a new [Wolf.Variant] under [name] after configuring it with [wolfVariant].
     */
    fun wolfVariant(name: String, wolfVariant: WolfVariantBuilder.() -> Unit): RegistryEntry.Paper<Wolf.Variant> =
        RegistryLoader.enqueueVanilla(RegistryKey.WOLF_VARIANT, key(this, name), ::WolfVariantBuilderImpl, wolfVariant)
    
    /**
     * Registers a new [Wolf.SoundVariant] under [name] after configuring it with [wolfSoundVariant].
     */
    fun wolfSoundVariant(name: String, wolfSoundVariant: WolfSoundVariantBuilder.() -> Unit): RegistryEntry.Paper<Wolf.SoundVariant> =
        RegistryLoader.enqueueVanilla(RegistryKey.WOLF_SOUND_VARIANT, key(this, name), ::WolfSoundVariantBuilderImpl, wolfSoundVariant)
    //</editor-fold>
    
    //<editor-fold desc="equipment">
    fun equipment(name: String, layout: StaticEquipmentLayoutBuilder.() -> Unit): RegistryEntry.Nova<Equipment> =
        registerEquipment(name) { StaticEquipmentLayoutBuilder(namespace(), it).apply(layout).build() }
    
    fun animatedEquipment(name: String, layout: AnimatedEquipmentLayoutBuilder.() -> Unit): RegistryEntry.Nova<Equipment> =
        registerEquipment(name) { AnimatedEquipmentLayoutBuilder(namespace(), it).apply(layout).build() }
    
    private fun registerEquipment(name: String, makeLayout: (ResourcePackBuilder) -> EquipmentLayout): RegistryEntry.Nova<Equipment> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_EQUIPMENT, key(this, name)) { Equipment(it, EquipmentTask.request(it, makeLayout)) }
    
    fun equipmentTag(name: String, configure: TagBuilder.Nova<Equipment>.() -> Unit): RegistryEntrySet.Nova.Tag<Equipment> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_EQUIPMENT, key(this, name), configure)
    
    fun equipmentTag(tag: RegistryEntrySet.Nova.Tag<Equipment>, configure: TagBuilder.Nova<Equipment>.() -> Unit): RegistryEntrySet.Nova.Tag<Equipment> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_EQUIPMENT, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="gui textures">
    fun guiTexture(name: String, guiTexture: GuiTextureBuilder.() -> Unit): RegistryEntry.Nova<GuiTexture> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_GUI_TEXTURE, key(this, name), ::GuiTextureBuilderImpl, guiTexture)
    
    fun guiTextureTag(name: String, configure: TagBuilder.Nova<GuiTexture>.() -> Unit): RegistryEntrySet.Nova.Tag<GuiTexture> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_GUI_TEXTURE, key(this, name), configure)
    
    fun guiTextureTag(tag: RegistryEntrySet.Nova.Tag<GuiTexture>, configure: TagBuilder.Nova<GuiTexture>.() -> Unit): RegistryEntrySet.Nova.Tag<GuiTexture> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_GUI_TEXTURE, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="item filter types">
    fun <T : ItemFilter<T>> registerItemFilterType(name: String, serializer: ItemFilterSerializer<T>): RegistryEntry.Nova<ItemFilterType<T>> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_ITEM_FILTER_TYPE, key(this, name)) { ItemFilterType(it, serializer) }
    
    fun itemFilterTypeTag(name: String, configure: TagBuilder.Nova<ItemFilterType<*>>.() -> Unit): RegistryEntrySet.Nova.Tag<ItemFilterType<*>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_ITEM_FILTER_TYPE, key(this, name), configure)
    
    fun itemFilterTypeTag(tag: RegistryEntrySet.Nova.Tag<ItemFilterType<*>>, configure: TagBuilder.Nova<ItemFilterType<*>>.() -> Unit): RegistryEntrySet.Nova.Tag<ItemFilterType<*>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_ITEM_FILTER_TYPE, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="items">
    fun item(name: String, item: NovaItemBuilder.() -> Unit): RegistryEntry.Nova<NovaItem> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_ITEM, key(this, name), ::NovaItemBuilderImpl, item)
    
    fun item(block: RegistryEntry.Nova<NovaBlock>, name: String = block.key.value(), item: NovaItemBuilder.() -> Unit): RegistryEntry.Nova<NovaItem> {
        require(block.key.namespace() == namespace()) { "The block must be from the same addon (block is from ${block.key.namespace()})!" }
        return RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_ITEM, key(this, name), { NovaItemBuilderImpl.fromBlock(it, block) }, item)
    }
    
    fun registerItem(
        name: String,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): RegistryEntry.Nova<NovaItem> = item(name) {
        behaviors(*behaviors)
        localizedName?.let(::localizedName)
        hidden(isHidden)
    }
    
    fun registerItem(
        block: RegistryEntry.Nova<NovaBlock>,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): RegistryEntry.Nova<NovaItem> = item(block) {
        behaviors(*behaviors)
        localizedName?.let(::localizedName)
        hidden(isHidden)
    }
    
    fun registerItem(
        block: RegistryEntry.Nova<NovaBlock>,
        name: String,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): RegistryEntry.Nova<NovaItem> = item(block, name) {
        behaviors(*behaviors)
        localizedName?.let(::localizedName)
        hidden(isHidden)
    }
    
    fun itemTag(name: String, configure: TagBuilder.Nova<NovaItem>.() -> Unit): RegistryEntrySet.Nova.Tag<NovaItem> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_ITEM, key(this, name), configure)
    
    fun itemTag(tag: RegistryEntrySet.Nova.Tag<NovaItem>, configure: TagBuilder.Nova<NovaItem>.() -> Unit): RegistryEntrySet.Nova.Tag<NovaItem> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_ITEM, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="network types">
    fun <T : Network<T>> registerNetworkType(
        name: String,
        createNetwork: NetworkConstructor<T>,
        createGroup: NetworkGroupConstructor<T>,
        validateLocal: LocalValidator,
        tickDelay: Provider<Int>,
        vararg holderTypes: KClass<out EndPointDataHolder>
    ): RegistryEntry.Nova<NetworkType<T>> = RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_NETWORK_TYPE, key(this, name)) {
        NetworkType(
            it,
            createNetwork, createGroup, validateLocal,
            tickDelay,
            holderTypes.toHashSet()
        )
    }
    
    fun networkTypeTag(name: String, configure: TagBuilder.Nova<NetworkType<*>>.() -> Unit): RegistryEntrySet.Nova.Tag<NetworkType<*>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_NETWORK_TYPE, key(this, name), configure)
    
    fun networkTypeTag(tag: RegistryEntrySet.Nova.Tag<NetworkType<*>>, configure: TagBuilder.Nova<NetworkType<*>>.() -> Unit): RegistryEntrySet.Nova.Tag<NetworkType<*>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_NETWORK_TYPE, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="recipe types">
    fun <T : NovaRecipe> registerRecipeType(name: String, recipeClass: KClass<T>, group: RecipeGroup<in T>, deserializer: RecipeDeserializer<T>?): RegistryEntry.Nova<RecipeType<T>> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_RECIPE_TYPE, key(this, name)) { RecipeType(it, recipeClass, group, deserializer) }
    
    fun recipeTypeTag(name: String, configure: TagBuilder.Nova<RecipeType<*>>.() -> Unit): RegistryEntrySet.Nova.Tag<RecipeType<*>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_RECIPE_TYPE, key(this, name), configure)
    
    fun recipeTypeTag(tag: RegistryEntrySet.Nova.Tag<RecipeType<*>>, configure: TagBuilder.Nova<RecipeType<*>>.() -> Unit): RegistryEntrySet.Nova.Tag<RecipeType<*>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_RECIPE_TYPE, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="tool">
    fun registerToolCategory(name: String): RegistryEntry.Nova<ToolCategory> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_TOOL_CATEGORY, key(this, name), ::ToolCategory)
    
    fun registerToolTier(name: String): RegistryEntry.Nova<ToolTier> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_TOOL_TIER, key(this, name)) { ToolTier(it, CONFIGS["${it.key.namespace()}:tool_levels"].entry(0.0, it.key.value())) }
    
    fun registerToolTier(name: String, level: Double): RegistryEntry.Nova<ToolTier> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_TOOL_TIER, key(this, name)) { ToolTier(it, provider(level)) }
    
    fun toolCategoryTag(name: String, configure: TagBuilder.Nova<ToolCategory>.() -> Unit): RegistryEntrySet.Nova.Tag<ToolCategory> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_TOOL_CATEGORY, key(this, name), configure)
    
    fun toolCategoryTag(tag: RegistryEntrySet.Nova.Tag<ToolCategory>, configure: TagBuilder.Nova<ToolCategory>.() -> Unit): RegistryEntrySet.Nova.Tag<ToolCategory> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_TOOL_CATEGORY, tag.tagKey, configure)
    
    fun toolTierTag(name: String, configure: TagBuilder.Nova<ToolTier>.() -> Unit): RegistryEntrySet.Nova.Tag<ToolTier> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_TOOL_TIER, key(this, name), configure)
    
    fun toolTierTag(tag: RegistryEntrySet.Nova.Tag<ToolTier>, configure: TagBuilder.Nova<ToolTier>.() -> Unit): RegistryEntrySet.Nova.Tag<ToolTier> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_TOOL_TIER, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="tooltip styles">
    /**
     * Registers a new [TooltipStyle] with the specified [name] and [meta].
     *
     * The tooltip textures are expected to be located under `textures/gui/sprites/tooltip/<name>_background.png` and
     * `textures/gui/sprites/tooltip/<name>_frame.png`. Their mcmeta can be configured via [meta].
     */
    fun tooltipStyle(name: String, meta: TooltipStyleLayoutBuilder.() -> Unit): RegistryEntry.Nova<TooltipStyle> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_TOOLTIP_STYLE, key(this, name)) { entry ->
            TooltipStyleTask.request(entry) { TooltipStyleLayoutBuilder(entry.key, it).apply(meta).build() }
            TooltipStyle(entry)
        }
    
    fun tooltipStyleTag(name: String, configure: TagBuilder.Nova<TooltipStyle>.() -> Unit): RegistryEntrySet.Nova.Tag<TooltipStyle> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_TOOLTIP_STYLE, key(this, name), configure)
    
    fun tooltipStyleTag(tag: RegistryEntrySet.Nova.Tag<TooltipStyle>, configure: TagBuilder.Nova<TooltipStyle>.() -> Unit): RegistryEntrySet.Nova.Tag<TooltipStyle> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_TOOLTIP_STYLE, tag.tagKey, configure)
    
    //</editor-fold>
    
    //<editor-fold desc="waila">
    /**
     * Registers a new [WailaInfoProvider] for vanilla blocks with the specified [name] after configuring it with [wailaInfoProvider].
     */
    @JvmName("wailaInfoProviderVanilla")
    fun <S : Any> wailaInfoProvider(
        name: String,
        wailaInfoProvider: WailaInfoProviderBuilder<BlockType, S>.() -> Unit
    ): RegistryEntry.Nova<WailaInfoProvider<BlockType, S>> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_WAILA_INFO_PROVIDER, key(this, name), ::WailaInfoProviderBuilderImpl, wailaInfoProvider)
    
    /**
     * Registers a new [WailaInfoProvider] for Nova blocks with the specified [name] after configuring it with [wailaInfoProvider].
     */
    @JvmName("wailaInfoProviderNova")
    fun wailaInfoProvider(
        name: String,
        wailaInfoProvider: WailaInfoProviderBuilder<NovaBlock, NovaBlockState>.() -> Unit
    ): RegistryEntry.Nova<WailaInfoProvider<NovaBlock, NovaBlockState>> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_WAILA_INFO_PROVIDER, key(this, name), ::WailaInfoProviderBuilderImpl, wailaInfoProvider)
    
    /**
     * Registers a new [WailaToolIconProvider] with the specified [name] after configuring it with [wailaToolIconProvider].
     */
    fun wailaToolIconProvider(
        name: String,
        wailaToolIconProvider: WailaToolIconProviderBuilder.() -> Unit
    ): RegistryEntry.Nova<WailaToolIconProvider> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_WAILA_TOOL_ICON_PROVIDER, key(this, name), ::WailaToolIconProviderBuilderImpl, wailaToolIconProvider)
    
    fun wailaInfoProviderTag(name: String, configure: TagBuilder.Nova<WailaInfoProvider<*, *>>.() -> Unit): RegistryEntrySet.Nova.Tag<WailaInfoProvider<*, *>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_WAILA_INFO_PROVIDER, key(this, name), configure)
    
    fun wailaInfoProviderTag(tag: RegistryEntrySet.Nova.Tag<WailaInfoProvider<*, *>>, configure: TagBuilder.Nova<WailaInfoProvider<*, *>>.() -> Unit): RegistryEntrySet.Nova.Tag<WailaInfoProvider<*, *>> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_WAILA_INFO_PROVIDER, tag.tagKey, configure)
    
    fun wailaToolIconProviderTag(name: String, configure: TagBuilder.Nova<WailaToolIconProvider>.() -> Unit): RegistryEntrySet.Nova.Tag<WailaToolIconProvider> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_WAILA_TOOL_ICON_PROVIDER, key(this, name), configure)
    
    fun wailaToolIconProviderTag(tag: RegistryEntrySet.Nova.Tag<WailaToolIconProvider>, configure: TagBuilder.Nova<WailaToolIconProvider>.() -> Unit): RegistryEntrySet.Nova.Tag<WailaToolIconProvider> =
        RegistryLoader.enqueueNovaTag(NovaRegistries.INTERNAL_WAILA_TOOL_ICON_PROVIDER, tag.tagKey, configure)
    //</editor-fold>
    
    //<editor-fold desc="worldgen">
    @ExperimentalWorldGen
    fun <T : RuleTest> registerRuleTestType(name: String, ruleTestType: RuleTestType<T>): RuleTestType<T> {
        val id = Identifier(this, name)
        Registries.RULE_TEST[id] = ruleTestType
        return ruleTestType
    }
    
    @ExperimentalWorldGen
    fun <T : RuleTest> registerRuleTestType(name: String, codec: MapCodec<T>): RuleTestType<T> =
        registerRuleTestType(name) { codec }
    
    @ExperimentalWorldGen
    fun biomeInjection(name: String, biomeInjection: BiomeInjectionBuilder.() -> Unit) {
        buildRegistryElementLater(namespace(), name, Registries.BIOME, ::BiomeInjectionBuilder, biomeInjection)
    }
    
    @ExperimentalWorldGen
    fun biome(name: String, biome: BiomeBuilder.() -> Unit): ResourceKey<Biome> =
        buildRegistryElementLater(namespace(), name, Registries.BIOME, ::BiomeBuilder, biome)
    
    @ExperimentalWorldGen
    fun <CC : CarverConfiguration> registerCarver(name: String, carver: WorldCarver<CC>): WorldCarver<CC> {
        val id = Identifier(this, name)
        Registries.CARVER[id] = carver
        return carver
    }
    
    @ExperimentalWorldGen
    fun <CC : CarverConfiguration> registerConfiguredCarver(name: String, configuredCarver: ConfiguredWorldCarver<CC>): ConfiguredWorldCarver<CC> {
        val id = Identifier(this, name)
        Registries.CONFIGURED_CARVER[id] = configuredCarver
        return configuredCarver
    }
    
    @ExperimentalWorldGen
    fun dimensionType(name: String, dimensionType: DimensionTypeBuilder.() -> Unit): ResourceKey<DimensionType> =
        buildRegistryElementLater(namespace(), name, Registries.DIMENSION_TYPE, ::DimensionTypeBuilder, dimensionType)
    
    @ExperimentalWorldGen
    fun placedFeature(name: String, placedFeature: PlacedFeatureBuilder.() -> Unit): ResourceKey<PlacedFeature> =
        buildRegistryElementLater(namespace(), name, Registries.PLACED_FEATURE, ::PlacedFeatureBuilder, placedFeature)
    
    @ExperimentalWorldGen
    fun <FC : FeatureConfiguration, F : Feature<FC>> configuredFeature(name: String, feature: F, config: FC): ResourceKey<ConfiguredFeature<*, *>> =
        configuredFeature(name, ConfiguredFeature(feature, config))
    
    @ExperimentalWorldGen
    fun <F : ConfiguredFeature<*, *>> configuredFeature(name: String, configuredFeature: F): ResourceKey<ConfiguredFeature<*, *>> {
        val key = ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier(this, name))
        Registries.CONFIGURED_FEATURE[key] = configuredFeature
        return key
    }
    
    @ExperimentalWorldGen
    fun feature(name: String, feature: Feature<*>): ResourceKey<Feature<*>> {
        val key = ResourceKey.create(Registries.FEATURE, Identifier(this, name))
        Registries.FEATURE[key] = feature
        return key
    }
    
    @ExperimentalWorldGen
    fun <P : PlacementModifier> placementModifierType(name: String, placementModifierType: PlacementModifierType<P>): PlacementModifierType<P> {
        val id = Identifier(this, name)
        Registries.PLACEMENT_MODIFIER_TYPE[id] = placementModifierType
        return placementModifierType
    }
    
    @ExperimentalWorldGen
    fun <P : PlacementModifier> placementModifierType(name: String, codec: MapCodec<P>): PlacementModifierType<P> =
        placementModifierType(name) { codec }
    
    @ExperimentalWorldGen
    fun registerNoiseParameters(name: String, noiseParams: NoiseParameters): NoiseParameters {
        val id = Identifier(this, name)
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
        val id = Identifier(this, name)
        Registries.NOISE_SETTINGS[id] = settings
        return settings
    }
    
    @ExperimentalWorldGen
    fun registerStructure(name: String, structure: Structure): Structure {
        val id = Identifier(this, name)
        Registries.STRUCTURE[id] = structure
        return structure
    }
    
    @ExperimentalWorldGen
    fun <P : StructurePoolElement> registerStructurePoolElementType(name: String, structurePoolElementType: StructurePoolElementType<P>): StructurePoolElementType<P> {
        val id = Identifier(this, name)
        Registries.STRUCTURE_POOL_ELEMENT[id] = structurePoolElementType
        return structurePoolElementType
    }
    
    @ExperimentalWorldGen
    fun registerStructurePieceType(name: String, structurePieceType: StructurePieceType): StructurePieceType {
        val id = Identifier(this, name)
        Registries.STRUCTURE_PIECE[id] = structurePieceType
        return structurePieceType
    }
    
    @ExperimentalWorldGen
    fun <SP : StructurePlacement> registerStructurePlacementType(name: String, structurePlacementType: StructurePlacementType<SP>): StructurePlacementType<SP> {
        val id = Identifier(this, name)
        Registries.STRUCTURE_PLACEMENT[id] = structurePlacementType
        return structurePlacementType
    }
    
    @ExperimentalWorldGen
    fun <P : StructureProcessor> registerStructureProcessorType(name: String, structureProcessorType: StructureProcessorType<P>): StructureProcessorType<P> {
        val id = Identifier(this, name)
        Registries.STRUCTURE_PROCESSOR[id] = structureProcessorType
        return structureProcessorType
    }
    
    @ExperimentalWorldGen
    fun registerStructureSet(name: String, structureSet: StructureSet): StructureSet {
        val id = Identifier(this, name)
        Registries.STRUCTURE_SET[id] = structureSet
        return structureSet
    }
    
    @ExperimentalWorldGen
    fun <S : Structure> registerStructureType(name: String, structureType: StructureType<S>): StructureType<S> {
        val id = Identifier(this, name)
        Registries.STRUCTURE_TYPE[id] = structureType
        return structureType
    }
    //</editor-fold>
    
}