package xyz.xenondevs.nova.data.serialization.cbf.adapter

import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.invui.inventory.VirtualInventory
import kotlin.reflect.KType

internal object VirtualInventoryBinaryAdapter : BinaryAdapter<VirtualInventory> {
    
    override fun read(type: KType, reader: ByteReader): VirtualInventory {
        val data = ByteArray(reader.readVarInt())
        reader.readBytes(data)
        return VirtualInventory.deserialize(data)
    }
    
    override fun write(obj: VirtualInventory, type: KType, writer: ByteWriter) {
        val data = obj.serialize()
        writer.writeVarInt(data.size)
        writer.writeBytes(data)
    }
    
}