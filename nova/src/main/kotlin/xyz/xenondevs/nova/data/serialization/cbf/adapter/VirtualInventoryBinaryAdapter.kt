package xyz.xenondevs.nova.data.serialization.cbf.adapter

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import java.lang.reflect.Type

internal object VirtualInventoryBinaryAdapter : BinaryAdapter<VirtualInventory> {
    
    override fun read(type: Type, reader: ByteReader): VirtualInventory {
        val data = ByteArray(reader.readVarInt())
        reader.readBytes(data)
        return VirtualInventoryManager.getInstance().deserializeInventory(data)
    }
    
    override fun write(obj: VirtualInventory, writer: ByteWriter) {
        val data = VirtualInventoryManager.getInstance().serializeInventory(obj)
        writer.writeVarInt(data.size)
        writer.writeBytes(data)
    }
    
}