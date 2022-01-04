package xyz.xenondevs.nova.data.serialization.cbf.element.other

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

class VirtualInventoryElement(override val value: VirtualInventory) : BackedElement<VirtualInventory>() {
    
    override fun getTypeId() = 26
    
    override fun write(buf: ByteBuf) {
        val data = VirtualInventoryManager.getInstance().serializeInventory(value)
        buf.writeInt(data.size)
        buf.writeBytes(data)
    }
    
    override fun toString() = value.toString()
    
}

object VirtualInventoryDeserializer : BinaryDeserializer<VirtualInventoryElement> {
    
    override fun read(buf: ByteBuf): VirtualInventoryElement {
        val data = ByteArray(buf.readInt())
        buf.readBytes(data)
        return VirtualInventoryElement(VirtualInventoryManager.getInstance().deserializeInventory(data))
    }
    
}