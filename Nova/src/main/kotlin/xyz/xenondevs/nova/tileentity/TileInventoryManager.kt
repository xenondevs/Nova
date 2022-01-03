package xyz.xenondevs.nova.tileentity

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import net.dzikoysk.exposed.upsert.upsert
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.database.DatabaseManager
import xyz.xenondevs.nova.data.database.asyncTransaction
import xyz.xenondevs.nova.data.database.table.TileInventoriesTable
import xyz.xenondevs.nova.util.runAsyncTaskTimer
import java.util.*

object TileInventoryManager {
    
    private val manager = VirtualInventoryManager.getInstance()
    private val inventories = HashMap<UUID, Pair<UUID, VirtualInventory>>()
    
    init {
        runAsyncTaskTimer(6000L, 6000L) { saveInventories() }
        NOVA.disableHandlers += ::saveInventories
    }
    
    @Synchronized
    fun loadInventory(tileEntityUUID: UUID, inventoryUUID: UUID, inventory: VirtualInventory) {
        if (inventoryUUID !in inventories)
            inventories[inventoryUUID] = tileEntityUUID to inventory
    }
    
    @Synchronized
    private fun saveInventories() {
        if (NOVA.isUninstalled)
            return
        transaction {
            inventories.forEach { (inventoryUUID, pair) ->
                val (tileEntityUUID, inventory) = pair
                TileInventoriesTable.upsert(
                    conflictColumn = TileInventoriesTable.id,
                    
                    insertBody = {
                        it[id] = inventoryUUID
                        it[tileEntityId] = tileEntityUUID
                        it[data] = inventory
                    },
                    
                    updateBody = {
                        it[data] = inventory
                    }
                )
            }
        }
    }
    
    @Synchronized
    fun remove(tileEntityUUID: UUID, inventories: List<VirtualInventory>) {
        inventories.forEach { this.inventories.remove(it.uuid) }
        
        if (!DatabaseManager.MYSQL) {
            asyncTransaction {
                TileInventoriesTable.deleteWhere { TileInventoriesTable.tileEntityId eq tileEntityUUID }
            }
        }
    }
    
    @Synchronized
    fun remove(inventory: VirtualInventory) {
        inventories.remove(inventory.uuid)
        
        if (!DatabaseManager.MYSQL) {
            asyncTransaction {
                TileInventoriesTable.deleteWhere { TileInventoriesTable.id eq inventory.uuid }
            }
        }
    }
    
    @Synchronized
    fun getOrCreate(tileEntityUUID: UUID, inventoryUUID: UUID, size: Int, items: Array<ItemStack?>, stackSizes: IntArray): VirtualInventory {
        return (getAndAddLegacyInventory(tileEntityUUID, inventoryUUID) ?: inventories.getOrPut(inventoryUUID)
        { tileEntityUUID to VirtualInventory(inventoryUUID, size, items, stackSizes) }).second
    }
    
    @Synchronized
    fun getByUuid(inventoryUUID: UUID): Pair<UUID, VirtualInventory>? =
        inventories[inventoryUUID]
    
    @Synchronized
    private fun getAndAddLegacyInventory(tileEntityUUID: UUID, inventoryUUID: UUID): Pair<UUID, VirtualInventory>? {
        return manager.getByUuid(inventoryUUID)
            ?.also { manager.remove(it) }
            ?.let { tileEntityUUID to it }
            ?.also { inventories[inventoryUUID] = it }
    }
    
}