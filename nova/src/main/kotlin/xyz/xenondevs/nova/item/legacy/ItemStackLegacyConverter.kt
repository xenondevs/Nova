package xyz.xenondevs.nova.item.legacy

import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.nova.util.data.getOrNull
import xyz.xenondevs.nova.util.item.novaCompound
import kotlin.reflect.KType
import net.minecraft.world.item.ItemStack as MojangStack

internal sealed interface ItemStackLegacyConverter {
    fun convert(itemStack: MojangStack)
}

internal interface SelectedItemStackLegacyConverter : ItemStackLegacyConverter {
    val affectedItemIds: Set<String>
}

internal interface AllItemStackLegacyConverter : ItemStackLegacyConverter

internal class ItemStackNamespaceConverter(
    override val affectedItemIds: Set<String>,
    private val newNamespace: String
) : SelectedItemStackLegacyConverter {
    
    override fun convert(itemStack: ItemStack) {
        val novaTag = itemStack.tag?.getOrNull<CompoundTag>("nova") ?: return
        novaTag.putString("id", newNamespace + ":" + novaTag.getString("id").split(':')[1])
    }
    
}

internal class ItemStackPersistentDataConverter(
    private val type: KType,
    private val oldKey: NamespacedKey,
    private val newKey: NamespacedKey = oldKey
) : AllItemStackLegacyConverter {
    
    override fun convert(itemStack: ItemStack) {
        val serializedValue = itemStack.tag
            ?.getOrNull<CompoundTag>("PublicBukkitValues")
            ?.getOrNull<ByteArrayTag>(oldKey.toString())
            ?.asByteArray
            ?: return
        
        itemStack.novaCompound[newKey] = CBF.read(type, serializedValue)
    }
    
}