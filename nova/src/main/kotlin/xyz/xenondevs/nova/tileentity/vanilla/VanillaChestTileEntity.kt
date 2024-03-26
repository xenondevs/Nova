package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.NOVA_PLUGIN
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.item.holder.DynamicVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedChestInventory
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.runTaskLaterSynchronized
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.pos
import java.util.*

internal class VanillaChestTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : ItemStorageVanillaTileEntity(pos, data) {
    
    override val type = Type.CHEST
    
    private lateinit var inventories: EnumMap<BlockFace, NetworkedInventory>
    private lateinit var allowedConnectionTypes: HashMap<NetworkedInventory, NetworkConnectionType>
    override val itemHolder: ItemHolder
    
    private var initialized = false
    private var doubleChestLocation: Location? = null
    
    init {
        setInventories()
        itemHolder = DynamicVanillaItemHolder(this, { inventories }, { allowedConnectionTypes })
        
        if (isChunkLoaded) {
            runTaskLaterSynchronized(VanillaTileEntityManager, 1) {
                if (!isChunkLoaded) return@runTaskLaterSynchronized
                doubleChestLocation = getOtherChestLocation()
                doubleChestLocation?.let {
                    val tileEntity = WorldDataManager.getVanillaTileEntity(it.pos)
                    if (tileEntity is VanillaChestTileEntity) tileEntity.handleChestStateChange()
                }
                handleChestStateChange()
            }
        }
    }
    
    // Should not be added to the NetworkManager before checking if it's a double chest
    override fun handleInitialized() = Unit
    
    private fun setInventories() {
        val chest = pos.block.state
        if (chest is Chest) {
            val inventory = NetworkedChestInventory(chest.inventory)
            inventories = CUBE_FACES.associateWithTo(enumMap()) { inventory }
            allowedConnectionTypes = inventories.entries.associateTo(HashMap()) { (_, inv) -> inv to NetworkConnectionType.BUFFER }
        }
    }
    
    private fun getOtherChestLocation(): Location? {
        val chest = pos.block.state
        if (chest is Chest) {
            val holder = chest.inventory.holder
            
            if (holder is DoubleChest) {
                val left = holder.leftSide as Chest
                val right = holder.rightSide as Chest
                
                return if (left.location == location) right.location else left.location
            }
        }
        
        return null
    }
    
    fun handleChestStateChange() {
        setInventories()
        NetworkManager.queueAsync {
            it.removeEndPoint(this, false)
            it.addEndPoint(this, false).thenRun {
                if (!initialized) {
                    initialized = true
                    updateNearbyBridges()
                }
            }
        }
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        
        if (NOVA_PLUGIN.isEnabled) {
            val doubleChestLocation = doubleChestLocation
            if (doubleChestLocation != null) {
                runTaskLaterSynchronized(VanillaTileEntityManager, 1) {
                    val chest = WorldDataManager.getVanillaTileEntity(doubleChestLocation.pos)
                    if (chest is VanillaChestTileEntity) chest.handleChestStateChange()
                }
            }
        }
    }
    
}