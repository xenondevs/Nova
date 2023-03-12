package xyz.xenondevs.nova.api.tileentity

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.material.NovaMaterial

interface TileEntity {
    
    /**
     * The owner of this [TileEntity]
     */
    val owner: OfflinePlayer?
    
    /**
     * The material of this [TileEntity]
     */
    val material: NovaMaterial
    
    /**
     * The location of this [TileEntity]
     */
    val location: Location
    
    /**
     * Retrieves a list of all [ItemStacks][ItemStack] this [TileEntity] would drop
     */
    fun getDrops(includeSelf: Boolean): MutableList<ItemStack>
    
}