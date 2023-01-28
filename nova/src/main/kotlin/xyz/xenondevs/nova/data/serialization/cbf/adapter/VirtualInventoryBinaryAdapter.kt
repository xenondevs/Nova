package xyz.xenondevs.nova.data.serialization.cbf.adapter

import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.invui.virtualinventory.VirtualInventory
import xyz.xenondevs.invui.virtualinventory.VirtualInventoryManager
import kotlin.reflect.KType

internal object VirtualInventoryBinaryAdapter : BinaryAdapter<VirtualInventory> {
    
    override fun read(type: KType, reader: ByteReader): VirtualInventory {
        val data = ByteArray(reader.readVarInt())
        reader.readBytes(data)
        return VirtualInventoryManager.getInstance().deserializeInventory(data)
    }
    
    override fun write(obj: VirtualInventory, type: KType, writer: ByteWriter) {
        val data = VirtualInventoryManager.getInstance().serializeInventory(obj)
        writer.writeVarInt(data.size)
        writer.writeBytes(data)
    }
    
}