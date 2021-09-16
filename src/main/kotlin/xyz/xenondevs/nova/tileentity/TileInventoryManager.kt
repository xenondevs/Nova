package xyz.xenondevs.nova.tileentity

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.database.DatabaseManager
import xyz.xenondevs.nova.data.database.asyncTransaction
import xyz.xenondevs.nova.data.database.table.TileInventoriesTable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

object TileInventoryManager {
    
    private val manager = VirtualInventoryManager.getInstance()
    private val inventories = HashMap<UUID, Pair<UUID, VirtualInventory>>()
    
    init {
        NOVA.disableHandlers += ::saveInventories
    }
    
    fun loadInventory(tileEntityUUID: UUID, inventoryUUID: UUID, data: ByteArray) {
        inventories[inventoryUUID] = tileEntityUUID to deserializeInventory(data)
    }
    
    private fun saveInventories() {
        if(NOVA.isUninstalled)
            return
        transaction {
            inventories.forEach { (inventoryUUID, pair) ->
                val (tileEntityUUID, inventory) = pair
                TileInventoriesTable.insert {
                    it[uuid] = inventoryUUID
                    it[tileEntityId] = tileEntityUUID
                    it[data] = ExposedBlob(serializeInventory(inventory))
                }
            }
        }
    }
    
    fun removeInventories(tileEntityUUID: UUID, inventories: List<VirtualInventory>) {
        inventories.forEach { this.inventories.remove(it.uuid) }
        
        if (!DatabaseManager.MYSQL) {
            asyncTransaction {
                TileInventoriesTable.deleteWhere { TileInventoriesTable.tileEntityId eq tileEntityUUID }
            }
        }
    }
    
    fun getOrCreate(tileEntityUUID: UUID, inventoryUUID: UUID, size: Int, items: Array<ItemStack?>, stackSizes: IntArray): VirtualInventory {
        val pair = manager.getByUuid(inventoryUUID)
            ?.also { manager.remove(it) }
            ?.let { tileEntityUUID to it }
            ?.also { inventories[inventoryUUID] = it }
            ?: inventories.getOrPut(inventoryUUID) { tileEntityUUID to VirtualInventory(inventoryUUID, size, items, stackSizes) }
        
        assert(pair.first == tileEntityUUID)
        return pair.second
    }
    
    private fun serializeInventory(inventory: VirtualInventory): ByteArray {
        val stream = ByteArrayOutputStream()
        manager.serializeInventory(inventory, stream)
        return stream.toByteArray()
    }
    
    private fun deserializeInventory(bytes: ByteArray): VirtualInventory =
        manager.deserializeInventory(ByteArrayInputStream(bytes))
    
}