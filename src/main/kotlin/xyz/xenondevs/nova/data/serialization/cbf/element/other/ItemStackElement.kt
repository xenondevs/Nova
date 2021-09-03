package xyz.xenondevs.nova.data.serialization.cbf.element.other

import de.studiocode.inventoryaccess.version.InventoryAccess
import io.netty.buffer.ByteBuf
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.BinaryDeserializer

class ItemStackElement(override val value: ItemStack) : BackedElement<ItemStack>() {
    
    override fun getTypeId() = 22
    
    override fun write(buf: ByteBuf) {
        val bytes = InventoryAccess.getItemUtils().serializeItemStack(value, true)
        require(bytes.size <= 65535) { "ItemStack data is too large" }
        buf.writeShort(bytes.size)
        buf.writeBytes(bytes)
    }
    
    override fun toString() = value.toString()
}

object ItemStackDeserializer : BinaryDeserializer<ItemStackElement> {
    override fun read(buf: ByteBuf): ItemStackElement {
        val bytes = ByteArray(buf.readUnsignedShort())
        buf.readBytes(bytes)
        val item = InventoryAccess.getItemUtils().deserializeItemStack(bytes, true)
        return ItemStackElement(item)
    }
}