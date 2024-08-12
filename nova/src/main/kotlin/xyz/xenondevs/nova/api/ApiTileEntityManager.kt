package xyz.xenondevs.nova.api

import org.bukkit.Location
import org.bukkit.World
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.nova.api.tileentity.TileEntityManager as ITileEntityManager

internal object ApiTileEntityManager : ITileEntityManager {
    
    override fun getTileEntity(location: Location): TileEntity? {
        return WorldDataManager.getTileEntity(location.pos)?.let(::ApiTileEntityWrapper)
    }
    
    override fun getTileEntities(world: World): List<TileEntity> {
        return WorldDataManager.getTileEntities(world).map(::ApiTileEntityWrapper)
    }
    
    override fun getTileEntities(): List<TileEntity> {
        return WorldDataManager.getTileEntities().map(::ApiTileEntityWrapper)
    }
    
}