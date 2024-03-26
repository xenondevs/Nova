package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.api.tileentity.TileEntity as ITileEntity

@Suppress("DEPRECATION")
internal class ApiTileEntityWrapper(val tileEntity: TileEntity) : ITileEntity {
    
    @Deprecated("Use NovaBlock instead", replaceWith = ReplaceWith("block"))
    override fun getMaterial(): NovaMaterial = LegacyMaterialWrapper(Either.right(tileEntity.block))
    override fun getBlock(): NovaBlock = ApiBlockWrapper(tileEntity.block)
    override fun getOwner(): OfflinePlayer? = tileEntity.owner
    override fun getLocation(): Location = tileEntity.pos.location
    override fun getDrops(includeSelf: Boolean): MutableList<ItemStack> = tileEntity.getDrops(includeSelf)
    
}