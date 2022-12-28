package xyz.xenondevs.nova.data.serialization.cbf.adapter

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.buffer.ByteBuffer
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import java.lang.reflect.Type

internal object ItemFilterBinaryAdapter : BinaryAdapter<ItemFilter> {
    
    override fun read(type: Type, buf: ByteBuffer): ItemFilter {
        val whitelist = buf.readBoolean()
        val nbt = buf.readBoolean()
        val size = buf.readVarInt()
        val items = Array<ItemStack?>(size) { CBF.read(buf) }
        
        return ItemFilter(whitelist, nbt, size, items)
    }
    
    override fun write(obj: ItemFilter, buf: ByteBuffer) {
        buf.writeBoolean(obj.whitelist)
        buf.writeBoolean(obj.nbt)
        buf.writeVarInt(obj.size)
        obj.items.forEach { CBF.write(it, buf) }
    }
    
}