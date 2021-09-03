package xyz.xenondevs.nova.data.serialization.cbf.element.other

import de.studiocode.inventoryaccess.version.InventoryAccess
import io.netty.buffer.ByteBuf
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

class ItemStackArrayElement(override val value: Array<ItemStack?>) : BackedElement<Array<ItemStack?>> {
    
    override fun getTypeId() = 26
    
    override fun write(buf: ByteBuf) {
        buf.writeShort(value.size)
        for (itemStack in value) {
            if (itemStack != null) {
                val serialized = InventoryAccess.getItemUtils().serializeItemStack(itemStack, true)
                buf.writeShort(serialized.size)
                buf.writeBytes(serialized)
            } else buf.writeShort(0)
        }
    }
    
    override fun toString(): String {
        return value.contentToString()
    }
    
}

object ItemStackArrayDeserializer : BinaryDeserializer<ItemStackArrayElement> {
    
    override fun read(buf: ByteBuf): ItemStackArrayElement {
        val valueSize = buf.readUnsignedShort()
        val array = arrayOfNulls<ItemStack>(valueSize)
        for (i in 0 until valueSize) {
            val length = buf.readUnsignedShort()
            if (length == 0) continue
            val serializedItemStack = ByteArray(length)
            buf.readBytes(serializedItemStack)
            array[i] = InventoryAccess.getItemUtils().deserializeItemStack(serializedItemStack, true)
        }
        
        return ItemStackArrayElement(array)
    }
    
}