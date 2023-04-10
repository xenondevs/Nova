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
internal class ApiTileEntityWrapper(private val tileEntity: TileEntity): ITileEntity {
    
    @Deprecated("Use NovaBlock instead", replaceWith = ReplaceWith("block"))
    override val material: NovaMaterial get() = LegacyMaterialWrapper(Either.right(tileEntity.block))
    override val block: NovaBlock get() = ApiBlockWrapper(tileEntity.block)
    override val owner: OfflinePlayer? get() = tileEntity.owner
    override val location: Location get() = tileEntity.location
    override fun getDrops(includeSelf: Boolean): MutableList<ItemStack> = tileEntity.getDrops(includeSelf)
    
}