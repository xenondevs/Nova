package xyz.xenondevs.nova.data.serialization.cbf.adapter

import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeTypeRegistry
import kotlin.reflect.KType

internal object UpgradeTypeBinaryAdapter : BinaryAdapter<UpgradeType<*>> {
    
    override fun read(type: KType, reader: ByteReader): UpgradeType<*> {
        return UpgradeTypeRegistry.of<Any>(NamespacedId.of(reader.readString()))!!
    }
    
    override fun write(obj: UpgradeType<*>, type: KType, writer: ByteWriter) {
        writer.writeString(obj.id.toString())
    }
    
}