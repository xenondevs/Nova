package xyz.xenondevs.nova.api

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.api.tileentity.TileEntity as ITileEntity

internal class ApiTileEntityWrapper(private val tileEntity: TileEntity): ITileEntity {
    
    /**
     * The owner of this [TileEntity]
     */
    override val owner: OfflinePlayer? get() = tileEntity.owner
    
    /**
     * The material of this [TileEntity]
     */
    override val material: NovaBlock get() = ApiBlockWrapper(tileEntity.material)
    
    /**
     * The location of this [TileEntity]
     */
    override val location: Location get() = tileEntity.location
    
    /**
     * Retrieves a list of all [ItemStacks][ItemStack] this [TileEntity] would drop
     */
    override fun getDrops(includeSelf: Boolean): MutableList<ItemStack> {
        return tileEntity.getDrops(includeSelf)
    }
    
}