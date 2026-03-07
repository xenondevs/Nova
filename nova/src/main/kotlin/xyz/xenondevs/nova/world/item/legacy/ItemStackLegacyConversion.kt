package xyz.xenondevs.nova.world.item.legacy

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.util.data.getCompoundOrNull
import xyz.xenondevs.nova.util.data.getStringOrNull
import kotlin.jvm.optionals.getOrNull

internal object ItemStackLegacyConversion {
    
    private val ENABLED by MAIN_CONFIG.entry<Boolean>("performance", "item_stack_legacy_conversion")
    
    private val specializedConverters = HashMap<String, ArrayList<ItemStackLegacyConverter>>()
    private val genericConverters = ArrayList<ItemStackLegacyConverter>()
    
    // there are no legacy converters at the moment
    
    private fun registerConverter(converter: ItemStackLegacyConverter) {
        val affectedItemIds = converter.affectedItemIds
        if (affectedItemIds != null) {
            for (affectedItemId in affectedItemIds) {
                specializedConverters.getOrPut(affectedItemId, ::ArrayList) += converter
            }
        } else {
            genericConverters += converter
        }
    }
    
    @Suppress("DEPRECATION")
    @JvmStatic
    fun convert(patch: DataComponentPatch): DataComponentPatch {
        if (!ENABLED)
            return patch
        
        val unsafeCustomTag = patch.get(DataComponents.CUSTOM_DATA)
            ?.getOrNull()
            ?.unsafe
            ?: return patch // not a nova item
        
        val novaId = unsafeCustomTag
            .getCompoundOrNull("nova")
            ?.getStringOrNull("id")
            ?: return patch // not a nova item
        
        val converters = (specializedConverters[novaId] ?: emptyList()) + genericConverters
        return converters.fold(patch) { acc, converter -> converter.convert(acc) }
    }
    
}