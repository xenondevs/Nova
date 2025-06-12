package xyz.xenondevs.nova.serialization.cbf

import net.kyori.adventure.key.Key
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.cbf.serializer.BinarySerializerFactory
import xyz.xenondevs.cbf.serializer.VersionedBinarySerializer
import xyz.xenondevs.commons.reflection.isSubtypeOf
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilterType
import xyz.xenondevs.nova.world.item.behavior.UnknownItemFilter
import kotlin.reflect.KType

private object ItemFilterBinarySerializer : VersionedBinarySerializer<ItemFilter<*>>(2U) {
    
    override fun readVersioned(version: UByte, reader: ByteReader): ItemFilter<*> {
        return when (version) {
            1.toUByte() -> readV1(reader)
            2.toUByte() -> readV2(reader)
            else -> throw UnsupportedOperationException()
        }
    }
    
    private fun readV2(reader: ByteReader): ItemFilter<*> {
        val filterTypeId = Key.key(reader.readString())
        return createFilter(
            filterTypeId,
            NovaRegistries.ITEM_FILTER_TYPE.getValue(filterTypeId),
            Cbf.read<Compound>(reader)!!
        )
    }
    
    private fun readV1(reader: ByteReader): ItemFilter<*> {
        val whitelist = reader.readBoolean()
        val nbt = reader.readBoolean()
        val size = reader.readVarInt()
        val items: List<ItemStack> = Array(size) { Cbf.read(reader) ?: ItemStack.empty() }.toList()
        
        val id = Key.key("logistics", if (nbt) "nbt_item_filter" else "type_item_filter")
        val compound = Compound()
        compound["items"] = items
        compound["whitelist"] = whitelist
        
        return createFilter(id, NovaRegistries.ITEM_FILTER_TYPE.getValue(id), compound)
    }
    
    private fun createFilter(id: Key, filterType: ItemFilterType<*>?, compound: Compound): ItemFilter<*> {
        if (filterType == null)
            return UnknownItemFilter(id, compound)
        return filterType.deserialize(compound)
    }
    
    override fun writeVersioned(obj: ItemFilter<*>, writer: ByteWriter) =
        write(obj, writer)
    
    @Suppress("UNCHECKED_CAST")
    private fun <T : ItemFilter<T>> write(filter: ItemFilter<T>, writer: ByteWriter) {
        if (filter is UnknownItemFilter) {
            writer.writeString(filter.originalId.toString())
            Cbf.write(filter.originalData, writer)
        } else {
            writer.writeString(NovaRegistries.ITEM_FILTER_TYPE.getKey(filter.type).toString())
            Cbf.write(filter.type.serialize(filter as T), writer)
        }
    }
    
    override fun copyNonNull(obj: ItemFilter<*>): ItemFilter<*> =
        copy(obj)
    
    @Suppress("UNCHECKED_CAST")
    private fun <T : ItemFilter<T>> copy(filter: ItemFilter<T>): ItemFilter<T> =
        filter.type.copy(filter as T)
    
}

internal object ItemFilterBinarySerializerFactory : BinarySerializerFactory {
    
    override fun create(type: KType): BinarySerializer<*>? {
        if (!type.isSubtypeOf<ItemFilter<*>?>())
            return null
        return ItemFilterBinarySerializer
    }
    
}