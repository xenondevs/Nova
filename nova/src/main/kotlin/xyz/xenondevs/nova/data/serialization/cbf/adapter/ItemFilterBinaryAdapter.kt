@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.data.serialization.cbf.adapter

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.item.behavior.UnknownItemFilter
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.type.item.ItemFilter
import kotlin.reflect.KType

internal object ItemFilterBinaryAdapter : BinaryAdapter<ItemFilter<*>> {
    
    override fun read(type: KType, reader: ByteReader): ItemFilter<*> {
        val id = ResourceLocation(reader.readString())
        val filterType = NovaRegistries.ITEM_FILTER_TYPE[id]
        val compound = CBF.read<Compound>(reader)!!
        
        if (filterType == null)
            return UnknownItemFilter(id, compound)
        return filterType.deserialize(compound)
    }
    
    override fun write(obj: ItemFilter<*>, type: KType, writer: ByteWriter) =
        write(obj, writer)
    
    private fun <T : ItemFilter<T>> write(filter: ItemFilter<T>, writer: ByteWriter) {
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