package xyz.xenondevs.nova.world.item.legacy

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.util.data.getCompoundOrNull
import xyz.xenondevs.nova.util.data.getStringOrNull
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.typeOf

internal object ItemStackLegacyConversion {
    
    private val ENABLED by MAIN_CONFIG.entry<Boolean>("performance", "item_stack_legacy_conversion")
    
    private val specializedConverters = HashMap<String, ArrayList<ItemStackLegacyConverter>>()
    private val genericConverters = ArrayList<ItemStackLegacyConverter>()
    
    // TODO: remove legacy conversions in future version
    init {
        registerConverter(ItemStackNamespaceConverter(
            hashSetOf(
                "nova:speed_upgrade",
                "nova:efficiency_upgrade",
                "nova:energy_upgrade",
                "nova:range_upgrade",
                "nova:fluid_upgrade"
            ),
            "simple_upgrades"
        ))
        
        registerConverter(ItemStackNamespaceConverter(
            hashSetOf("nova:wrench"),
            "logistics"
        ))
        
        registerConverter(ItemStackPersistentDataConverter(
            typeOf<Compound>(),
            NamespacedKey("nova", "tileentitydata"),
            NamespacedKey("nova", "tileentity")
        ))
        
        registerConverter(ItemStackPersistentDataConverter(
            typeOf<Long>(),
            NamespacedKey("nova", "item_energy"),
            NamespacedKey("nova", "energy")
        ))
        
        registerConverter(ItemStackPersistentDataConverter(
            typeOf<Compound>(),
            NamespacedKey("nova", "damage"),
        ))
        
        registerConverter(ItemStackNovaDamageConverter)
        registerConverter(ItemStackEnchantmentsConverter)
    }
    
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