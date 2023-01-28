package xyz.xenondevs.nova.data.serialization.cbf.adapter

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import java.lang.reflect.Type

internal object ItemFilterBinaryAdapter : BinaryAdapter<ItemFilter> {
    
    override fun read(type: Type, reader: ByteReader): ItemFilter {
        val whitelist = reader.readBoolean()
        val nbt = reader.readBoolean()
        val size = reader.readVarInt()
        val items = Array<ItemStack?>(size) { CBF.read(reader) }
        
        return ItemFilter(whitelist, nbt, size, items)
    }
    
    override fun write(obj: ItemFilter, writer: ByteWriter) {
        writer.writeBoolean(obj.whitelist)
        writer.writeBoolean(obj.nbt)
        writer.writeVarInt(obj.size)
        obj.items.forEach { CBF.write(it, writer) }
    }
    
}