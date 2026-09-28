package xyz.xenondevs.nova.world.item.legacy

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.component.CustomData
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.util.getOrNull
import xyz.xenondevs.nova.util.data.getCompoundOrNull
import xyz.xenondevs.nova.util.data.getStringOrNull
import xyz.xenondevs.nova.world.item.NovaItem

internal object ItemStackLegacyConversion {
    
    private val ENABLED by MAIN_CONFIG.entry<Boolean>("performance", "item_stack_legacy_conversion")
    
    @Suppress("DEPRECATION")
    @JvmStatic
    fun convert(item: Holder<Item>, patch: DataComponentPatch): ItemStackLegacyConversionResult? {
        if (!ENABLED)
            return null
        
        val prototype = item.components()
        val customTag = patch.get(prototype, DataComponents.CUSTOM_DATA)
            ?.unsafe
            ?.copy()
            ?: return null
        var changed = ItemStackNamespacedCompoundConverter.convert(customTag)
        
        val novaId = customTag
            .getCompoundOrNull("nova")
            ?.getStringOrNull("id")
        val legacyItem = novaId
            ?.let(BuiltInRegistries.ITEM::getOrNull)
            ?.takeIf { it.value() is NovaItem }
        val convertedItem = legacyItem ?: item
        if (legacyItem != null) {
            customTag.remove("nova")
            changed = true
        }
        
        if (!changed)
            return null
        
        val convertedPatch = DataComponentPatch.builder().apply {
            copy(patch)
            if (customTag.isEmpty) {
                remove(DataComponents.CUSTOM_DATA)
            } else {
                set(DataComponents.CUSTOM_DATA, CustomData.of(customTag))
            }
        }.build()
        
        return ItemStackLegacyConversionResult(convertedItem, convertedPatch)
    }
    
}

internal data class ItemStackLegacyConversionResult(
    val item: Holder<Item>,
    val components: DataComponentPatch
)