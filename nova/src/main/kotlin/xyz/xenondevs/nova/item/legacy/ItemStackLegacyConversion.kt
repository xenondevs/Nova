package xyz.xenondevs.nova.item.legacy

import net.minecraft.nbt.CompoundTag
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.NOVA_PLUGIN
import kotlin.reflect.typeOf

internal object ItemStackLegacyConversion {
    
    private val idConverters = HashMap<String, ArrayList<ItemStackLegacyConverter>>()
    private val allConverters = ArrayList<ItemStackLegacyConverter>()
    
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
            NamespacedKey(NOVA_PLUGIN, "tileEntityData"),
            NamespacedKey(NOVA_PLUGIN, "tileentity")
        ))
        
        registerConverter(ItemStackPersistentDataConverter(
            typeOf<Long>(),
            NamespacedKey(NOVA_PLUGIN, "item_energy"),
            NamespacedKey(NOVA_PLUGIN, "energy")
        ))
        
        registerConverter(ItemStackPersistentDataConverter(
            typeOf<Compound>(),
            NamespacedKey(NOVA_PLUGIN, "damage"),
        ))
    }
    
    private fun registerConverter(converter: ItemStackLegacyConverter) {
        when (converter) {
            is SelectedItemStackLegacyConverter -> {
                converter.affectedItemIds.forEach { id ->
                    idConverters.getOrPut(id, ::ArrayList) += converter
                }
            }
            
            is AllItemStackLegacyConverter -> allConverters += converter
        }
    }
    
    fun convert(customTag: CompoundTag, novaId: String) {
        idConverters[novaId]?.forEach { it.convert(customTag) }
        allConverters.forEach { it.convert(customTag) }
    }
    
}