package xyz.xenondevs.nova.data.serialization.cbf.adapter

import de.studiocode.inventoryaccess.version.InventoryAccess
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.buffer.ByteBuffer
import java.lang.reflect.Type

internal object ItemStackBinaryAdapter: BinaryAdapter<ItemStack> {
    
    override fun read(type: Type, buf: ByteBuffer): ItemStack {
        val data = ByteArray(buf.readVarInt())
        buf.readBytes(data)
        return InventoryAccess.getItemUtils().deserializeItemStack(data, true)
    }
    
    override fun write(obj: ItemStack, buf: ByteBuffer) {
        val data = InventoryAccess.getItemUtils().serializeItemStack(obj, true)
        buf.writeVarInt(data.size)
        buf.writeBytes(data)
    }
    
    
}