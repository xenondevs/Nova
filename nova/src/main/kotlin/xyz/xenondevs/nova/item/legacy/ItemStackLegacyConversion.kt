package xyz.xenondevs.nova.item.legacy

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.data.getOrNull
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
    
    @JvmStatic // Called via reflection in LegacyConversionPatch
    fun convert(itemStack: ItemStack) {
        val tag = itemStack.tag?.getOrNull<CompoundTag>("nova") ?: return
        val id = tag.getString("id")
        
        idConverters[id]?.forEach { it.convert(itemStack) }
        allConverters.forEach { it.convert(itemStack) }
    }
    
}