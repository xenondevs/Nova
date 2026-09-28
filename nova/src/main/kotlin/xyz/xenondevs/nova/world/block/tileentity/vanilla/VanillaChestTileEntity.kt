package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.core.SectionPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.ChestType
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.DynamicVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.DoubleChestItemStackContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer
import xyz.xenondevs.nova.world.chunkPos

internal class VanillaChestTileEntity internal constructor(
    private val chestEntity: ChestBlockEntity
) : ItemStorageVanillaTileEntity(chestEntity) {
    
    @Volatile
    private var inventoryLayout = InventoryLayout(singleInventory())
    
    @Volatile
    private var linkedChest: VanillaChestTileEntity? = null
    
    private var chestType = ChestType.SINGLE
    
    @Volatile
    private var linkedNodeSet: Set<NetworkNode> = emptySet()
    
    override val linkedNodes: Set<NetworkNode>
        get() = linkedNodeSet
    
    override val itemHolder: ItemHolder = DynamicVanillaItemHolder(
        chestEntity,
        { inventoryLayout.inventories },
        { inventoryLayout.allowedConnectionTypes }
    )
    
    override fun handleCreated() {
        refreshLink()
    }
    
    internal fun refreshLink() {
        if (chestEntity.isRemoved) {
            handleRemoved()
            return
        }
        
        val chestType = chestEntity.blockState.getValue(BlockStateProperties.CHEST_TYPE)
        val linkedEntity = findLinkedChestEntity(chestType)
        val linked = linkedEntity?.let(VanillaTileEntity::of) as? VanillaChestTileEntity
        val previous = linkedChest
        
        setLink(linked, chestType)
        
        if (previous !== linked && previous?.linkedChest === this) {
            previous.setLink(null, ChestType.SINGLE)
        }
        
        linked?.setLink(this, chestType.opposite)
    }
    
    internal fun handleRemoved() {
        val linked = linkedChest
        setLink(null, ChestType.SINGLE)
        
        if (linked?.linkedChest === this)
            linked.setLink(null, ChestType.SINGLE)
    }
    
    private fun setLink(linked: VanillaChestTileEntity?, chestType: ChestType) {
        if (linkedChest === linked && this.chestType == chestType)
            return
        
        linkedChest = linked
        this.chestType = chestType
        linkedNodeSet = linked?.let(::setOf) ?: emptySet()
        
        val inventory = createNetworkedInventory(linked, chestType)
        inventoryLayout = InventoryLayout(CubeFaceMap(inventory))
        
        NetworkManager.queue(block.chunkPos) { state ->
            if (this !in state)
                return@queue false
            
            state.forEachNetwork(this) { _, _, network ->
                network.markDirty()
                network.cluster?.invalidate()
            }
            true
        }
    }
    
    private fun createNetworkedInventory(linked: VanillaChestTileEntity?, chestType: ChestType): NetworkedInventory {
        if (linked == null || chestType == ChestType.SINGLE)
            return NetworkedNMSInventory(SimpleItemStackContainer(chestEntity.contents), chestEntity)
        
        val left: MutableList<ItemStack>
        val right: MutableList<ItemStack>
        if (chestType == ChestType.LEFT) {
            left = chestEntity.contents
            right = linked.chestEntity.contents
        } else {
            left = linked.chestEntity.contents
            right = chestEntity.contents
        }
        
        return NetworkedNMSInventory(DoubleChestItemStackContainer(left, right), chestEntity, linked.chestEntity)
    }
    
    private fun singleInventory(): CubeFaceMap<NetworkedInventory> =
        CubeFaceMap(NetworkedNMSInventory(SimpleItemStackContainer(chestEntity.contents), chestEntity))
    
    private fun findLinkedChestEntity(chestType: ChestType): ChestBlockEntity? {
        if (chestType == ChestType.SINGLE)
            return null
        
        val level = chestEntity.level as? ServerLevel
            ?: return null
        val facing = chestEntity.blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        val linkedPos = chestEntity.blockPos.relative(ChestBlock.getConnectedDirection(chestEntity.blockState))
        val linkedChunk = level.getChunkIfLoaded(
            SectionPos.blockToSectionCoord(linkedPos.x),
            SectionPos.blockToSectionCoord(linkedPos.z)
        ) ?: return null
        val linked = linkedChunk.getBlockEntity(linkedPos) as? ChestBlockEntity
            ?: return null
        if (linked.isRemoved || linked.type != chestEntity.type)
            return null
        
        val linkedState = linked.blockState
        if (
            linkedState.block !is ChestBlock ||
            linkedState.getValue(BlockStateProperties.CHEST_TYPE) != chestType.opposite ||
            linkedState.getValue(BlockStateProperties.HORIZONTAL_FACING) != facing
        ) return null
        
        return linked
    }
    
    companion object {
        
        fun isLinkStateChanged(oldState: BlockState, newState: BlockState): Boolean {
            return oldState.getValue(BlockStateProperties.CHEST_TYPE) != newState.getValue(BlockStateProperties.CHEST_TYPE) ||
                oldState.getValue(BlockStateProperties.HORIZONTAL_FACING) != newState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        }
        
    }
    
    private class InventoryLayout(val inventories: CubeFaceMap<NetworkedInventory>) {
        val allowedConnectionTypes: Map<NetworkedInventory, NetworkConnectionType> =
            inventories.values.associateWith { NetworkConnectionType.BUFFER }
    }
    
}
