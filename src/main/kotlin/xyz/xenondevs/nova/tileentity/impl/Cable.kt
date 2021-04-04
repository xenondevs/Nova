package xyz.xenondevs.nova.tileentity.impl

import com.google.common.base.Preconditions
import org.bukkit.Axis
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Orientable
import org.bukkit.entity.ArmorStand
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.energy.*
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.MultiModelTileEntity
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.blockLocation
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.particle.ParticleBuilder
import xyz.xenondevs.particle.ParticleEffect

private const val CONNECTOR = 1
private const val HORIZONTAL = 2
private const val DOWN = 3
private const val UP = 4

class Cable(
    material: NovaMaterial,
    armorStand: ArmorStand
) : MultiModelTileEntity(
    material,
    armorStand,
    keepData = false
), EnergyBridge {
    
    override var network: EnergyNetwork? = null
    override val transferRate = 100
    override val bridgeFaces = CUBE_FACES.toSet() // TODO: allow players to enable / disable cable faces
    
    private var _connectedNodes: Map<BlockFace, EnergyNode>? = null
    override val connectedNodes: Map<BlockFace, EnergyNode>
        get() {
            if (_connectedNodes == null) _connectedNodes = findConnectedNodes()
            return _connectedNodes!!
        }
    
    override fun handleTick() {
        if (network != null) {
            ParticleBuilder(ParticleEffect.REDSTONE, armorStand.location)
                .setParticleData(network!!.color)
                .display()
        }
    }
    
    override fun handleNetworkUpdate() {
        _connectedNodes = findConnectedNodes()
        if (NOVA.isEnabled) {
            replaceModels(getModelsNeeded())
            updateHitbox()
        }
    }
    
    override fun handleInitialized() {
        EnergyNetworkManager.handleBridgeAdd(this)
        replaceModels(getModelsNeeded())
        updateHitbox()
    }
    
    override fun handleRemoved(unload: Boolean) {
        EnergyNetworkManager.handleBridgeRemove(this, unload)
    }
    
    private fun getModelsNeeded(): List<Pair<ItemStack, Float>> {
        Preconditions.checkState(network != null, "Network is not initialized")
        
        val items = ArrayList<Pair<ItemStack, Float>>()
        
        // get all nodes that are connected to the same network as this cable
        val neighboringNodes = armorStand.location.blockLocation.getNearbyNodes().filter { (face, node) ->
            node.getNetwork(face.oppositeFace) == network
        }.keys.toList()
        
        // only show connector if connections aren't on two opposite sides
        if (neighboringNodes.size != 2 || neighboringNodes[0] != neighboringNodes[1].oppositeFace) {
            items += material.block!!.getItem(CONNECTOR) to 0f
        }
        
        // add all connections
        neighboringNodes.forEach { blockFace ->
            val dataIndex = when (blockFace) {
                BlockFace.DOWN -> DOWN
                BlockFace.UP -> UP
                else -> HORIZONTAL
            }
            
            val rotation = when (blockFace) {
                BlockFace.NORTH -> 0f
                BlockFace.EAST -> 90f
                BlockFace.SOUTH -> 180f
                BlockFace.WEST -> 270f
                else -> 0f
            }
            
            val itemStack = material.block!!.getItem(dataIndex)
            items += itemStack to rotation
        }
        
        return items
    }
    
    private fun updateHitbox() {
        val block = armorStand.location.block
        
        val neighborFaces = block.location.getNearbyNodes()
            .map { (blockFace, _) -> blockFace }
        val axis = when {
            neighborFaces.contains(BlockFace.NORTH) && neighborFaces.contains(BlockFace.SOUTH) -> Axis.Z
            neighborFaces.contains(BlockFace.UP) && neighborFaces.contains(BlockFace.DOWN) -> Axis.Y
            else -> Axis.X
        }
        
        // run later because this might be called during an event
        runTaskLater(1) {
            block.type = Material.CHAIN
            val blockData = block.blockData as Orientable
            blockData.axis = axis
            block.setBlockData(blockData, false)
        }
    }
    
    override fun saveData() = Unit
    
    override fun handleRightClick(event: PlayerInteractEvent) = Unit // TODO: Configuration Menu
    
}