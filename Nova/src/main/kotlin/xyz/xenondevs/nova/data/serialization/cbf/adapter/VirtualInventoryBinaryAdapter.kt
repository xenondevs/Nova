package xyz.xenondevs.nova.data.serialization.cbf.adapter

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import java.lang.reflect.Type

internal object VirtualInventoryBinaryAdapter : BinaryAdapter<VirtualInventory> {
    
    override fun write(obj: VirtualInventory, buf: ByteBuf) {
        val data = VirtualInventoryManager.getInstance().serializeInventory(obj)
        buf.writeInt(data.size)
        buf.writeBytes(data)
    }
    
    override fun read(type: Type, buf: ByteBuf): VirtualInventory {
        val data = ByteArray(buf.readInt())
        buf.readBytes(data)
        return VirtualInventoryManager.getInstance().deserializeInventory(data)
    }
    
    
}