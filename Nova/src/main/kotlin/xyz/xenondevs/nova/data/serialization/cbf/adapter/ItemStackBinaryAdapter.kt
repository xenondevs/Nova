package xyz.xenondevs.nova.data.serialization.cbf.adapter

import de.studiocode.inventoryaccess.version.InventoryAccess
import io.netty.buffer.ByteBuf
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import java.lang.reflect.Type

internal object ItemStackBinaryAdapter : BinaryAdapter<ItemStack> {
    
    override fun write(obj: ItemStack, buf: ByteBuf) {
        val data = InventoryAccess.getItemUtils().serializeItemStack(obj, true)
        buf.writeInt(data.size)
        buf.writeBytes(data)
    }
    
    override fun read(type: Type, buf: ByteBuf): ItemStack {
        val data = ByteArray(buf.readInt())
        buf.readBytes(data)
        return InventoryAccess.getItemUtils().deserializeItemStack(data, true)
    }
    
}