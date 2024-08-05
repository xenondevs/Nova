package xyz.xenondevs.nova.tileentity.vanilla

import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.ChestType
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.item.holder.DefaultItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.holder.DynamicVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla.DoubleChestItemStackContainer
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.util.*

internal class VanillaChestTileEntity internal constructor(
    type: Type,
    pos: BlockPos,
    data: Compound
) : ItemStorageVanillaTileEntity(type, pos, data) {
    
    private lateinit var inventories: EnumMap<BlockFace, NetworkedInventory>
    private lateinit var allowedConnectionTypes: HashMap<NetworkedInventory, NetworkConnectionType>
    override lateinit var itemHolder: ItemHolder
    
    private var chestType: ChestType = ChestType.SINGLE
    private var linkedChest: VanillaChestTileEntity? = null
    
    override fun handleEnable() {
        linkedChest = getLinkedChest()?.also(linkedNodes::add)
        setInventory(createNetworkedInventory())
        
        DefaultItemHolder.tryConvertLegacy(this)?.let { storeData("itemHolder", it) } // legacy conversion
        itemHolder = DynamicVanillaItemHolder(
            storedValue("itemHolder", ::Compound),
            { inventories },
            { allowedConnectionTypes }
        )
        
        super.handleEnable()
    }
    
    override fun handlePlace() {
        linkedChest?.handleChestLink(this, chestType.opposite)
        super.handlePlace()
    }
    
    override fun handleBreak() {
        super.handleBreak()
        linkedChest?.handleChestLink(null, ChestType.SINGLE)
    }
    
    private fun handleChestLink(newLink: VanillaChestTileEntity?, newType: ChestType) {
        linkedChest = newLink
        chestType = newType
        
        linkedNodes.clear()
        if (newLink != null)
            linkedNodes += newLink
        
        val inventory = createNetworkedInventory()
        
        NetworkManager.queueWrite(pos.world) { state ->
            setInventory(inventory)
            state.forEachNetwork(this) { _, _, network -> network.markDirty() }
        }
    }
    
    private fun getLinkedChest(): VanillaChestTileEntity? {
        checkServerThread()
        
        val blockState = pos.nmsBlockState
        val chestType = blockState.getValue(BlockStateProperties.CHEST_TYPE)
        this.chestType = chestType
        if (chestType == ChestType.SINGLE)
            return null
        
        val facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        val linkedPos = when {
            chestType == ChestType.LEFT && facing == Direction.NORTH -> pos.add(1, 0, 0)
            chestType == ChestType.LEFT && facing == Direction.EAST -> pos.add(0, 0, 1)
            chestType == ChestType.LEFT && facing == Direction.SOUTH -> pos.add(-1, 0, 0)
            chestType == ChestType.LEFT && facing == Direction.WEST -> pos.add(0, 0, -1)
            chestType == ChestType.RIGHT && facing == Direction.NORTH -> pos.add(-1, 0, 0)
            chestType == ChestType.RIGHT && facing == Direction.EAST -> pos.add(0, 0, -1)
            chestType == ChestType.RIGHT && facing == Direction.SOUTH -> pos.add(1, 0, 0)
            chestType == ChestType.RIGHT && facing == Direction.WEST -> pos.add(0, 0, 1)
            else -> throw IllegalArgumentException("Invalid chest type $chestType and facing $facing")
        }
        
        return WorldDataManager.getVanillaTileEntity(linkedPos) as? VanillaChestTileEntity
    }
    
    private fun createNetworkedInventory(): NetworkedInventory {
        checkServerThread()
        
        val chest = pos.nmsBlockEntity as ChestBlockEntity
        val linkedChest = linkedChest
        val chestType = chestType
        if (chestType == ChestType.SINGLE || linkedChest == null)
            return NetworkedNMSInventory(SimpleItemStackContainer(chest.contents))
        
        val left: MutableList<ItemStack>
        val right: MutableList<ItemStack>
        when (chestType) {
            ChestType.LEFT -> {
                left = chest.contents
                right = (linkedChest.pos.nmsBlockEntity as ChestBlockEntity).contents
            }
            
            ChestType.RIGHT -> {
                left = (linkedChest.pos.nmsBlockEntity as ChestBlockEntity).contents
                right = chest.contents
            }
            
            else -> throw UnsupportedOperationException()
        }
        
        return NetworkedNMSInventory(DoubleChestItemStackContainer(left, right))
    }
    
    private fun setInventory(inventory: NetworkedInventory) {
        inventories = CUBE_FACES.associateWithTo(enumMap()) { inventory }
        allowedConnectionTypes = inventories.entries.associateTo(HashMap()) { (_, inv) -> inv to NetworkConnectionType.BUFFER }
    }
    
}