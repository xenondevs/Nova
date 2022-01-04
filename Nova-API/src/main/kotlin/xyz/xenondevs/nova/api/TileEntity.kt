package xyz.xenondevs.nova.api

import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack

interface TileEntity {
    
    val owner: OfflinePlayer
    
    fun getDrops(includeSelf: Boolean): MutableList<ItemStack>
    
}