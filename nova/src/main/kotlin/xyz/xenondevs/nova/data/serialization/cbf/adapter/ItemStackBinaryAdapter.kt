package xyz.xenondevs.nova.data.serialization.cbf.adapter

import de.studiocode.inventoryaccess.version.InventoryAccess
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import kotlin.reflect.KType

internal object ItemStackBinaryAdapter: BinaryAdapter<ItemStack> {
    
    override fun read(type: KType, reader: ByteReader): ItemStack {
        val data = ByteArray(reader.readVarInt())
        reader.readBytes(data)
        return InventoryAccess.getItemUtils().deserializeItemStack(data, true)
    }
    
    override fun write(obj: ItemStack, type: KType, writer: ByteWriter) {
        val data = InventoryAccess.getItemUtils().serializeItemStack(obj, true)
        writer.writeVarInt(data.size)
        writer.writeBytes(data)
    }
    
    
}