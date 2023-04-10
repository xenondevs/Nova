package xyz.xenondevs.nova.api

import org.bukkit.Location
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.api.tileentity.TileEntityManager as ITileEntityManager

internal object ApiTileEntityManager : ITileEntityManager {
    
    override fun getTileEntity(location: Location): TileEntity? {
        return TileEntityManager.getTileEntity(location)?.let(::ApiTileEntityWrapper)
    }
    
}