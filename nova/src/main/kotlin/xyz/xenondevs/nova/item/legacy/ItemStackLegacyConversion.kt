package xyz.xenondevs.nova.item.legacy

import net.minecraft.nbt.CompoundTag
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.NOVA
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