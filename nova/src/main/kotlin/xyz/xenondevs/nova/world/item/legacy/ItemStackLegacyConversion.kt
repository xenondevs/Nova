package xyz.xenondevs.nova.world.item.legacy

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.data.getCompoundOrNull
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.typeOf

internal object ItemStackLegacyConversion {
    
    private val specializedConverters = HashMap<String, ArrayList<ItemStackLegacyConverter>>()
    private val genericConverters = ArrayList<ItemStackLegacyConverter>()
    
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
            NamespacedKey(NOVA, "tileEntityData"),
            NamespacedKey(NOVA, "tileentity")
        ))
        
        registerConverter(ItemStackPersistentDataConverter(
            typeOf<Long>(),
            NamespacedKey(NOVA, "item_energy"),
            NamespacedKey(NOVA, "energy")
        ))
        
        registerConverter(ItemStackPersistentDataConverter(
            typeOf<Compound>(),
            NamespacedKey(NOVA, "damage"),
        ))
        
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
        val unsafeCustomTag = patch.get(DataComponents.CUSTOM_DATA)
            ?.getOrNull()
            ?.unsafe
            ?: return patch // not a nova item
        
        val novaId = unsafeCustomTag
            .getCompoundOrNull("nova")
            ?.getString("id")
            ?: return patch // not a nova item
        
        val converters = (specializedConverters[novaId] ?: emptyList()) + genericConverters
        return converters.fold(patch) { acc, converter -> converter.convert(acc) }
    }
    
}