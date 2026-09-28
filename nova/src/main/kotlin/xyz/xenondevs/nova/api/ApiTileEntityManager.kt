package xyz.xenondevs.nova.api

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.novaTileEntities
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.api.tileentity.TileEntityManager as ITileEntityManager

internal object ApiTileEntityManager : ITileEntityManager {
    
    override fun getTileEntity(location: Location): TileEntity? {
        return location.block.novaTileEntity?.let(::ApiTileEntityWrapper)
    }
    
    override fun getTileEntities(chunk: Chunk): List<TileEntity> {
        return chunk.novaTileEntities.map(::ApiTileEntityWrapper)
    }
    
    override fun getTileEntities(world: World): List<TileEntity> {
        return world.loadedChunks.flatMap(::getTileEntities)
    }
    
    override fun getTileEntities(): List<TileEntity> {
        return Bukkit.getWorlds().flatMap(::getTileEntities)
    }
    
}