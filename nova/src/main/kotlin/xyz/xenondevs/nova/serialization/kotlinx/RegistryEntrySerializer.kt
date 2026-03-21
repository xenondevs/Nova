package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilterType
import xyz.xenondevs.nova.world.item.Equipment
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.recipe.RecipeType
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.player.ability.AbilityType
import xyz.xenondevs.nova.world.player.attachment.AttachmentType

internal object BlockSerializer : NmsRegistryEntrySerializer<Block>(BuiltInRegistries.BLOCK)

internal abstract class NmsRegistryEntrySerializer<T : Any>(val registry: Registry<T>) : KSerializer<T> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.NmsRegistryEntrySerializer", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: T) {
        val id = registry.getKey(value)
            ?: throw SerializationException("Value $value is not registered in $registry")
        encoder.encodeString(id.toString())
    }
    
    override fun deserialize(decoder: Decoder): T {
        val id = decoder.decodeString()
        return registry.getValue(id)
            ?: throw SerializationException("No entry under $id in $registry")
    }
    
}

/**
 * Serializer for [NovaBlock], serializes by [NovaBlock.key] in the format of `namespace:value`.
 */
object NovaBlockSerializer : NovaRegistryElementSerializer<NovaBlock>(NovaRegistries.BLOCK)

/**
 * Serializer for [NovaItem], serializes by [NovaItem.key] in the format of `namespace:value`.
 */
object NovaItemSerializer : NovaRegistryElementSerializer<NovaItem>(NovaRegistries.ITEM)

/**
 * Serializer for [Equipment], serializes by [Equipment.key] in the format of `namespace:value`.
 */
object EquipmentSerializer : NovaRegistryElementSerializer<Equipment>(NovaRegistries.EQUIPMENT)

/**
 * Serializer for [ToolTier], serializes by [ToolTier.key] in the format of `namespace:value`.
 */
object ToolTierSerializer : NovaRegistryElementSerializer<ToolTier>(NovaRegistries.TOOL_TIER)

/**
 * Serializer for [ToolCategory], serializes by [ToolCategory.key] in the format of `namespace:value`.
 */
object ToolCategorySerializer : NovaRegistryElementSerializer<ToolCategory>(NovaRegistries.TOOL_CATEGORY)

/**
 * Serializer for [NetworkType], serializes by [NetworkType.key] in the format of `namespace:value`.
 */
object NetworkTypeSerializer : NovaRegistryElementSerializer<NetworkType<*>>(NovaRegistries.NETWORK_TYPE)

/**
 * Serializer for [AbilityType], serializes by [AbilityType.key] in the format of `namespace:value`.
 */
object AbilityTypeSerializer : NovaRegistryElementSerializer<AbilityType<*>>(NovaRegistries.ABILITY_TYPE)

/**
 * Serializer for [AttachmentType], serializes by [AttachmentType.key] in the format of `namespace:value`.
 */
object AttachmentTypeSerializer : NovaRegistryElementSerializer<AttachmentType<*>>(NovaRegistries.ATTACHMENT_TYPE)

/**
 * Serializer for [RecipeType], serializes by [RecipeType.key] in the format of `namespace:value`.
 */
object RecipeTypeSerializer : NovaRegistryElementSerializer<RecipeType<*>>(NovaRegistries.RECIPE_TYPE)

/**
 * Serializer for [GuiTexture], serializes by [GuiTexture.key] in the format of `namespace:value`.
 */
object GuiTextureSerializer : NovaRegistryElementSerializer<GuiTexture>(NovaRegistries.GUI_TEXTURE)

/**
 * Serializer for [WailaInfoProvider], serializes by [WailaInfoProvider.key] in the format of `namespace:value`.
 */
object WailaInfoProviderSerializer : NovaRegistryElementSerializer<WailaInfoProvider<*, *>>(NovaRegistries.WAILA_INFO_PROVIDER)

/**
 * Serializer for [WailaToolIconProvider], serializes by [WailaToolIconProvider.key] in the format of `namespace:value`.
 */
object WailaToolIconProviderSerializer : NovaRegistryElementSerializer<WailaToolIconProvider>(NovaRegistries.WAILA_TOOL_ICON_PROVIDER)

/**
 * Serializer for [ItemFilterType], serializes by [ItemFilterType.key] in the format of `namespace:value`.
 */
object ItemFilterTypeSerializer : NovaRegistryElementSerializer<ItemFilterType<*>>(NovaRegistries.ITEM_FILTER_TYPE)

/**
 * Serializer for [TooltipStyle], serializes by [TooltipStyle.key] in the format of `namespace:value`.
 */
object TooltipStyleSerializer : NovaRegistryElementSerializer<TooltipStyle>(NovaRegistries.TOOLTIP_STYLE)

/**
 * Serializer for [RegistryEntry.Nova] of [NovaBlock], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object NovaBlockEntrySerializer : NovaRegistryEntrySerializer<NovaBlock>(NovaRegistries.BLOCK)

/**
 * Serializer for [RegistryEntry.Nova] of [NovaItem], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object NovaItemEntrySerializer : NovaRegistryEntrySerializer<NovaItem>(NovaRegistries.ITEM)

/**
 * Serializer for [RegistryEntry.Nova] of [Equipment], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object EquipmentEntrySerializer : NovaRegistryEntrySerializer<Equipment>(NovaRegistries.EQUIPMENT)

/**
 * Serializer for [RegistryEntry.Nova] of [ToolTier], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object ToolTierEntrySerializer : NovaRegistryEntrySerializer<ToolTier>(NovaRegistries.TOOL_TIER)

/**
 * Serializer for [RegistryEntry.Nova] of [ToolCategory], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object ToolCategoryEntrySerializer : NovaRegistryEntrySerializer<ToolCategory>(NovaRegistries.TOOL_CATEGORY)

/**
 * Serializer for [RegistryEntry.Nova] of [NetworkType], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object NetworkTypeEntrySerializer : NovaRegistryEntrySerializer<NetworkType<*>>(NovaRegistries.NETWORK_TYPE)

/**
 * Serializer for [RegistryEntry.Nova] of [AbilityType], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object AbilityTypeEntrySerializer : NovaRegistryEntrySerializer<AbilityType<*>>(NovaRegistries.ABILITY_TYPE)

/**
 * Serializer for [RegistryEntry.Nova] of [AttachmentType], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object AttachmentTypeEntrySerializer : NovaRegistryEntrySerializer<AttachmentType<*>>(NovaRegistries.ATTACHMENT_TYPE)

/**
 * Serializer for [RegistryEntry.Nova] of [RecipeType], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object RecipeTypeEntrySerializer : NovaRegistryEntrySerializer<RecipeType<*>>(NovaRegistries.RECIPE_TYPE)

/**
 * Serializer for [RegistryEntry.Nova] of [GuiTexture], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object GuiTextureEntrySerializer : NovaRegistryEntrySerializer<GuiTexture>(NovaRegistries.GUI_TEXTURE)

/**
 * Serializer for [RegistryEntry.Nova] of [WailaInfoProvider], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object WailaInfoProviderEntrySerializer : NovaRegistryEntrySerializer<WailaInfoProvider<*, *>>(NovaRegistries.WAILA_INFO_PROVIDER)

/**
 * Serializer for [RegistryEntry.Nova] of [WailaToolIconProvider], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object WailaToolIconProviderEntrySerializer : NovaRegistryEntrySerializer<WailaToolIconProvider>(NovaRegistries.WAILA_TOOL_ICON_PROVIDER)

/**
 * Serializer for [RegistryEntry.Nova] of [ItemFilterType], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object ItemFilterTypeEntrySerializer : NovaRegistryEntrySerializer<ItemFilterType<*>>(NovaRegistries.ITEM_FILTER_TYPE)

/**
 * Serializer for [RegistryEntry.Nova] of [TooltipStyle], serialized by [RegistryEntry.key] in the format of `namespace:value`.
 */
object TooltipStyleEntrySerializer : NovaRegistryEntrySerializer<TooltipStyle>(NovaRegistries.TOOLTIP_STYLE)