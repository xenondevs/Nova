@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.api

import com.mojang.datafixers.util.Either
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.craftbukkit.block.CraftBlockType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock
import xyz.xenondevs.nova.api.tileentity.TileEntity as ITileEntity

internal class ApiTileEntityWrapper(val tileEntity: TileEntity) : ITileEntity {
    
    @Deprecated("Use NovaBlock instead", replaceWith = ReplaceWith("block"))
    override fun getMaterial(): NovaMaterial = LegacyMaterialWrapper(Either.right((tileEntity.blockType as CraftBlockType<*>).handle as NovaBlock))
    override fun getBlock(): INovaBlock = ApiBlockWrapper((tileEntity.blockType as CraftBlockType<*>).handle as NovaBlock)
    override fun getOwner(): OfflinePlayer? = tileEntity.owner
    override fun getLocation(): Location = tileEntity.block.location
    override fun getDrops(includeSelf: Boolean): List<ItemStack> = tileEntity.getDrops(includeSelf)
    
}
