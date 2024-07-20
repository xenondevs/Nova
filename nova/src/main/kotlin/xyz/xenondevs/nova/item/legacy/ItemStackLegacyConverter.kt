package xyz.xenondevs.nova.item.legacy

import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.CompoundTag
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.util.data.getByteArrayOrNull
import xyz.xenondevs.nova.util.data.getOrNull
import kotlin.reflect.KType

internal sealed interface ItemStackLegacyConverter {
    fun convert(customData: CompoundTag)
}

internal interface SelectedItemStackLegacyConverter : ItemStackLegacyConverter {
    val affectedItemIds: Set<String>
}

internal interface AllItemStackLegacyConverter : ItemStackLegacyConverter

internal class ItemStackNamespaceConverter(
    override val affectedItemIds: Set<String>,
    private val newNamespace: String
) : SelectedItemStackLegacyConverter {
    
    override fun convert(customData: CompoundTag) {
        val novaTag = customData.getOrNull<CompoundTag>("nova") ?: return
        novaTag.putString("id", newNamespace + ":" + novaTag.getString("id").split(':')[1])
    }
    
}

internal class ItemStackPersistentDataConverter(
    private val type: KType,
    private val oldKey: NamespacedKey,
    private val newKey: NamespacedKey = oldKey
) : AllItemStackLegacyConverter {
    
    override fun convert(customData: CompoundTag) {
        val serializedValue = customData
            .getOrNull<CompoundTag>("PublicBukkitValues")
            ?.getOrNull<ByteArrayTag>(oldKey.toString())
            ?.asByteArray
            ?: return
        val novaCompound = customData
            .getByteArrayOrNull("nova_cbf")
            ?.let(CBF::read)
            ?: NamespacedCompound()
        novaCompound.set(type, newKey, CBF.read(type, serializedValue))
        customData.putByteArray("nova_cbf", CBF.write(novaCompound))
    }
    
}