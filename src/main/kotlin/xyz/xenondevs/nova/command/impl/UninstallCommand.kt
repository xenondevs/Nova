package xyz.xenondevs.nova.command.impl

import net.minecraft.world.level.storage.LevelResource
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.command.Command
import xyz.xenondevs.nova.command.executesCatching
import xyz.xenondevs.nova.command.requiresConsole
import xyz.xenondevs.nova.data.database.table.TileEntitiesTable
import xyz.xenondevs.nova.data.database.table.TileInventoriesTable
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.util.capitalize
import xyz.xenondevs.nova.util.minecraftServer
import java.io.File

// TODO only load tileentities that have multiple hitboxes
object UninstallCommand : Command("nvuninstall") {
    
    init {
        builder = builder
            .requiresConsole()
            .executesCatching { uninstall() }
            .apply {
                Bukkit.getWorlds().forEach { world ->
                    then(literal(world.name).executesCatching { uninstallWorld(world) })
                }
            }
        
    }
    
    fun uninstall() {
        TileEntityManager.tileEntityChunks.forEach(TileEntityManager::saveChunk)
        transaction {
            TileEntitiesTable.selectAll()
                .map { getOrCreateTileEntity(it) }
                .forEach { TileEntityManager.destroyTileEntity(it, dropItems = false) }
            
            SchemaUtils.drop(TileEntitiesTable, TileInventoriesTable)
        }
        File(minecraftServer.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), "bukkit/data/${NOVA.name.lowercase()}").deleteRecursively()
        NOVA.dataFolder.deleteRecursively()
        NOVA.isUninstalled = true
        Bukkit.getPluginManager().disablePlugin(NOVA)
        LOGGER.info("Your server has been cleared of all Nova content except items.")
        LOGGER.warning("Please remember to stop your server and delete " + NOVA.pluginFile.name + "!")
    }
    
    fun uninstallWorld(world: World) {
        if (Bukkit.getWorld(world.uid) == null) {
            LOGGER.warning(world.name.capitalize() + " doesn't exist anymore.")
            return
        }
        TileEntityManager.tileEntityChunks.filter { it.world == world }.forEach(TileEntityManager::saveChunk)
        transaction {
            TileEntitiesTable
                .select { TileEntitiesTable.world eq world.uid }
                .map { getOrCreateTileEntity(it) }
                .forEach { TileEntityManager.destroyTileEntity(it, dropItems = false) }
            
            TileEntitiesTable.deleteWhere { TileEntitiesTable.world eq world.uid }
            LOGGER.info(world.name.capitalize() + " has been cleared of all Nova blocks. Please note that items still exist!")
        }
    }
    
    
    private fun getOrCreateTileEntity(row: ResultRow): TileEntity {
        val location = Location(
            Bukkit.getWorld(row[TileEntitiesTable.world]),
            row[TileEntitiesTable.x].toDouble(),
            row[TileEntitiesTable.y].toDouble(),
            row[TileEntitiesTable.z].toDouble(),
        )
        var tileEntity = TileEntityManager.getTileEntityAt(location, false)
        if (tileEntity == null) {
            tileEntity = TileEntity.create(row, location)
            tileEntity.handleInitialized(false)
        }
        return tileEntity
    }
}