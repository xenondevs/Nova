package xyz.xenondevs.nova.data.serialization.cbf.adapter

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.buffer.ByteBuffer
import java.lang.reflect.Type

internal object VirtualInventoryBinaryAdapter : BinaryAdapter<VirtualInventory> {
    
    override fun read(type: Type, buf: ByteBuffer): VirtualInventory {
        val data = ByteArray(buf.readVarInt())
        buf.readBytes(data)
        return VirtualInventoryManager.getInstance().deserializeInventory(data)
    }
    
    override fun write(obj: VirtualInventory, buf: ByteBuffer) {
        val data = VirtualInventoryManager.getInstance().serializeInventory(obj)
        buf.writeVarInt(data.size)
        buf.writeBytes(data)
    }
    
}