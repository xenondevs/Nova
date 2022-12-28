package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter

import io.netty.buffer.ByteBuf
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.BinaryAdapterLegacy
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.CBFLegacy
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.LegacyCompound
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import java.lang.reflect.Type

internal object ItemFilterBinaryAdapterLegacy : BinaryAdapterLegacy<ItemFilter> {
    
    override fun write(obj: ItemFilter, buf: ByteBuf) {
        val compound = LegacyCompound().also {
            val itemList = obj.items.toList()
            it["items"] = itemList
            it["nbt"] = obj.nbt
            it["whitelist"] = obj.whitelist
        }
        CBFLegacy.write(compound)
    }
    
    override fun read(type: Type, buf: ByteBuf): ItemFilter {
        buf.readerIndex(buf.readerIndex() - 1)
        val compound = CBFLegacy.read<LegacyCompound>(buf)!!
        val items: Array<ItemStack?> = compound.get<List<ItemStack>>("items")!!.toTypedArray()
        return ItemFilter(
            compound["whitelist"]!!,
            compound["nbt"] ?: false,
            items.size,
            items
        )
    }
    
}