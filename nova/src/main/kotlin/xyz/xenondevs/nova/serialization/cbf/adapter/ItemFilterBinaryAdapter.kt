@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.serialization.cbf.adapter

import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.adapter.ComplexBinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilterType
import xyz.xenondevs.nova.world.item.behavior.UnknownItemFilter
import kotlin.reflect.KType

internal object ItemFilterBinaryAdapter : ComplexBinaryAdapter<ItemFilter<*>> {
    
    override fun read(type: KType, id: UByte, reader: ByteReader): ItemFilter<*> {
        if (id == 1.toUByte())
            return readLegacy(reader)
        
        val filterTypeId = ResourceLocation.parse(reader.readString())
        return createFilter(
            filterTypeId,
            NovaRegistries.ITEM_FILTER_TYPE[filterTypeId],
            CBF.read<Compound>(reader)!!
        )
    }
    
    private fun readLegacy(reader: ByteReader): ItemFilter<*> {
        val whitelist = reader.readBoolean()
        val nbt = reader.readBoolean()
        val size = reader.readVarInt()
        val items: List<ItemStack> = Array(size) { CBF.read(reader) ?: ItemStack.empty() }.toList()
        
        val id = ResourceLocation.fromNamespaceAndPath("logistics", if (nbt) "nbt_item_filter" else "type_item_filter")
        val compound = Compound()
        compound["items"] = items
        compound["whitelist"] = whitelist
        
        return createFilter(id, NovaRegistries.ITEM_FILTER_TYPE[id], compound)
    }
    
    private fun createFilter(id: ResourceLocation, filterType: ItemFilterType<*>?, compound: Compound): ItemFilter<*> {
        if (filterType == null)
            return UnknownItemFilter(id, compound)
        return filterType.deserialize(compound)
    }
    
    override fun write(obj: ItemFilter<*>, type: KType, writer: ByteWriter) =
        write(obj, writer)
    
    private fun <T : ItemFilter<T>> write(filter: ItemFilter<T>, writer: ByteWriter) {
        writer.writeUnsignedByte(2.toUByte())
        
        if (filter is UnknownItemFilter) {
            writer.writeString(filter.originalId.toString())
            CBF.write(filter.originalData, writer)
        } else {
            writer.writeString(NovaRegistries.ITEM_FILTER_TYPE.getKey(filter.type).toString())
            CBF.write(filter.type.serialize(filter as T), writer)
        }
    }
    
    override fun copy(obj: ItemFilter<*>, type: KType): ItemFilter<*> =
        copy(obj)
    
    private fun <T : ItemFilter<T>> copy(filter: ItemFilter<T>): ItemFilter<T> =
        filter.type.copy(filter as T)
    
}