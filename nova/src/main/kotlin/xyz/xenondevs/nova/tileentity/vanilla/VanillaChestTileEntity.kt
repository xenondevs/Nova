package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.BlockFace
import org.bukkit.inventory.Inventory
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.item.holder.DynamicVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedBukkitInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedDoubleChestInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.util.*
import org.bukkit.block.Chest as ChestBlockEntity
import org.bukkit.block.data.type.Chest as ChestBlockData

internal class VanillaChestTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : ItemStorageVanillaTileEntity(pos, data) {
    
    override val type = Type.CHEST
    
    private lateinit var inventories: EnumMap<BlockFace, NetworkedInventory>
    private lateinit var allowedConnectionTypes: HashMap<NetworkedInventory, NetworkConnectionType>
    override var itemHolder: ItemHolder = DynamicVanillaItemHolder(
        storedValue("itemHolder", ::Compound), // TODO: legacy support
        { inventories },
        { allowedConnectionTypes }
    )
    
    private var chestType = ChestBlockData.Type.SINGLE
    private var linkedChest: VanillaChestTileEntity? = null
    
    override fun handleEnable() {
        linkedChest = getLinkedChest()?.also(linkedNodes::add)
        setInventory(createNetworkedInventory())
    }
    
    override fun handlePlace() {
        linkedChest?.handleChestLink(this, getOtherChestType(chestType))
        super.handlePlace()
    }
    
    override fun handleBreak() {
        super.handleBreak()
        linkedChest?.handleChestLink(null, ChestBlockData.Type.SINGLE)
    }
    
    private fun handleChestLink(newLink: VanillaChestTileEntity?, newType: ChestBlockData.Type) {
        linkedChest = newLink
        chestType = newType

        linkedNodes.clear()
        if (newLink != null)
            linkedNodes += newLink
        
        val inventory = createNetworkedInventory()
        
        NetworkManager.queueWrite(pos.world) { state ->
            setInventory(inventory)
            state.forEachNetwork(this) { _, _, network -> network.dirty = true }
        }
    }
    
    private fun getLinkedChest(): VanillaChestTileEntity? {
        checkServerThread()
        
        val blockData = pos.block.blockData as ChestBlockData
        val chestType = blockData.type
        this.chestType = chestType
        if (chestType == ChestBlockData.Type.SINGLE)
            return null
        
        val facing = blockData.facing
        val linkedPos = when {
            chestType == ChestBlockData.Type.LEFT && facing == BlockFace.NORTH -> pos.add(1, 0, 0)
            chestType == ChestBlockData.Type.LEFT && facing == BlockFace.EAST -> pos.add(0, 0, 1)
            chestType == ChestBlockData.Type.LEFT && facing == BlockFace.SOUTH -> pos.add(-1, 0, 0)
            chestType == ChestBlockData.Type.LEFT && facing == BlockFace.WEST -> pos.add(0, 0, -1)
            chestType == ChestBlockData.Type.RIGHT && facing == BlockFace.NORTH -> pos.add(-1, 0, 0)
            chestType == ChestBlockData.Type.RIGHT && facing == BlockFace.EAST -> pos.add(0, 0, -1)
            chestType == ChestBlockData.Type.RIGHT && facing == BlockFace.SOUTH -> pos.add(1, 0, 0)
            chestType == ChestBlockData.Type.RIGHT && facing == BlockFace.WEST -> pos.add(0, 0, 1)
            else -> throw IllegalArgumentException("Invalid chest type $chestType and facing $facing")
        }
        
        return WorldDataManager.getVanillaTileEntity(linkedPos) as? VanillaChestTileEntity
    }
    
    private fun createNetworkedInventory(): NetworkedInventory {
        checkServerThread()
        
        val chest = pos.block.state as ChestBlockEntity
        val linkedChest = linkedChest
        val chestType = chestType
        if (chestType == ChestBlockData.Type.SINGLE || linkedChest == null)
            return NetworkedBukkitInventory(chest.blockInventory)
        
        val left: Inventory
        val right: Inventory
        when (chestType) {
            ChestBlockData.Type.LEFT -> {
                left = chest.blockInventory
                right = (linkedChest.pos.block.state as ChestBlockEntity).blockInventory
            }
            
            ChestBlockData.Type.RIGHT -> {
                left = (linkedChest.pos.block.state as ChestBlockEntity).blockInventory
                right = chest.blockInventory
            }
            
            else -> throw UnsupportedOperationException()
        }
        
        return NetworkedDoubleChestInventory(left, right)
    }
    
    private fun getOtherChestType(type: ChestBlockData.Type): ChestBlockData.Type =
        when (type) {
            ChestBlockData.Type.LEFT -> ChestBlockData.Type.RIGHT
            ChestBlockData.Type.RIGHT -> ChestBlockData.Type.LEFT
            ChestBlockData.Type.SINGLE -> ChestBlockData.Type.SINGLE
        }
    
    private fun setInventory(inventory: NetworkedInventory) {
        inventories = CUBE_FACES.associateWithTo(enumMap()) { inventory }
        allowedConnectionTypes = inventories.entries.associateTo(HashMap()) { (_, inv) -> inv to NetworkConnectionType.BUFFER }
    }
    
}