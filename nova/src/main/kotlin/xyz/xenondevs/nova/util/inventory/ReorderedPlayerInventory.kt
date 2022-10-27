package xyz.xenondevs.nova.util.inventory

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

// TODO: implement currently unsupported methods
internal class ReorderedPlayerInventory(private val inventory: PlayerInventory): PlayerInventory by inventory {
    
    override fun setItem(index: Int, item: ItemStack?) {
        inventory.setItem(toChronologicalSlots(index), item)
    }
    
    override fun getItem(index: Int): ItemStack? {
        return inventory.getItem(toChronologicalSlots(index))
    }
    
    override fun setContents(items: Array<out ItemStack?>) {
        inventory.contents = Array(items.size) { items[fromChronologicalSlots(it)] }
    }
    
    override fun getContents(): Array<ItemStack?> {
        val contents = inventory.contents
        return Array(contents.size) { contents[toChronologicalSlots(it)] }
    }
    
    override fun setStorageContents(items: Array<out ItemStack?>) {
        inventory.storageContents = Array(items.size) { items[fromChronologicalSlots(it)] }
    }
    
    override fun getStorageContents(): Array<ItemStack?> {
        val storageContents = inventory.storageContents
        return Array(storageContents.size) { storageContents[toChronologicalSlots(it)] }
    }
    
    private fun toChronologicalSlots(slot: Int): Int =
        if (slot > 26) slot - 27 else slot + 9
    
    private fun fromChronologicalSlots(slot: Int): Int =
        if (slot > 8) slot - 9 else slot + 27
    
    override fun all(item: ItemStack?): HashMap<Int, out ItemStack> {
        throw UnsupportedOperationException()
    }
    
    override fun all(material: Material): HashMap<Int, out ItemStack> {
        throw UnsupportedOperationException()
    }
    
    override fun addItem(vararg items: ItemStack?): HashMap<Int, ItemStack> {
        throw UnsupportedOperationException()
    }
    
    override fun first(item: ItemStack): Int {
        throw UnsupportedOperationException()
    }
    
    override fun first(material: Material): Int {
        throw UnsupportedOperationException()
    }
    
    override fun firstEmpty(): Int {
        throw UnsupportedOperationException()
    }
    
    override fun iterator(): MutableListIterator<ItemStack> {
        throw UnsupportedOperationException()
    }
    
    override fun iterator(index: Int): MutableListIterator<ItemStack> {
        throw UnsupportedOperationException()
    }
    
}