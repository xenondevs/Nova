package xyz.xenondevs.nova.data.serialization.cbf.adapter

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.invui.util.ItemUtils
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import kotlin.reflect.KType

internal object ItemFilterBinaryAdapter : BinaryAdapter<ItemFilter> {
    
    override fun read(type: KType, reader: ByteReader): ItemFilter {
        val whitelist = reader.readBoolean()
        val nbt = reader.readBoolean()
        val size = reader.readVarInt()
        val items = Array<ItemStack?>(size) { CBF.read(reader) }
        
        return ItemFilter(whitelist, nbt, size, items)
    }
    
    override fun write(obj: ItemFilter, type: KType, writer: ByteWriter) {
        writer.writeBoolean(obj.whitelist)
        writer.writeBoolean(obj.nbt)
        writer.writeVarInt(obj.size)
        obj.items.forEach { CBF.write(it, writer) }
    }
    
    override fun copy(obj: ItemFilter, type: KType): ItemFilter {
        return ItemFilter(obj.whitelist, obj.nbt, obj.size, ItemUtils.clone(obj.items))
    }
    
}