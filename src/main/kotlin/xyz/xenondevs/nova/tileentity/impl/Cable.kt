package xyz.xenondevs.nova.tileentity.impl

import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Orientable
import org.bukkit.entity.ArmorStand
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.MultiModelTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.getBlockLocation
import xyz.xenondevs.nova.util.runTaskLater

private const val CONNECTOR = 1
private const val HORIZONTAL = 2
private const val DOWN = 3
private const val UP = 4

private fun getModelsNeeded(cableLocation: Location, material: NovaMaterial): List<Pair<ItemStack, Float>> {
    val items = ArrayList<Pair<ItemStack, Float>>()
    
    val neighboringCables = getNeighboringCables(cableLocation, material)
    
    // only show connector if connections aren't on two opposite sides
    if (neighboringCables.size != 2 || neighboringCables[0].first != neighboringCables[1].first.oppositeFace) {
        items += material.block!!.getItem(CONNECTOR) to 0f
    }
    
    // add all connections
    getNeighboringCables(cableLocation, material)
        .forEach { (blockFace, _) ->
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

private fun getNeighboringCables(cableLocation: Location, material: NovaMaterial): List<Pair<BlockFace, Cable>> {
    return CUBE_FACES
        .mapNotNull {
            val location = cableLocation.clone().add(it.modX.toDouble(), it.modY.toDouble(), it.modZ.toDouble())
            val tileEntity = TileEntityManager.getTileEntityAt(location)
            if (tileEntity is Cable && tileEntity.material == material) it to tileEntity else null
        }
}

class Cable(
    material: NovaMaterial,
    armorStand: ArmorStand
) : MultiModelTileEntity(
    material,
    armorStand,
    getModelsNeeded(armorStand.location.getBlockLocation(), material),
    keepData = false
) {
    
    init {
        // update models of neighboring cables so they connect to this cable
        updateNeighboringCables()
        
        // TODO: notify something like an EnergyNetworkManager of this cable
    }
    
    private fun updateNeighboringCables() {
        runTaskLater(1) { // needs to be a tick later or this cable won't visible for the other ones
            getNeighboringCables(armorStand.location.getBlockLocation(), material).forEach { (_, cable) ->
                val models = getModelsNeeded(cable.armorStand.location.getBlockLocation(), cable.material)
                cable.replaceModels(models)
            }
        }
    }
    
    private fun updateHitbox() {
        val block = armorStand.location.block
        
        val neighborFaces = getNeighboringCables(block.location, material)
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
    
    override fun setModels(models: List<Pair<ItemStack, Float>>) {
        super.setModels(models)
        
        // set hitbox
        updateHitbox()
    }
    
    override fun destroy(dropItems: Boolean): java.util.ArrayList<ItemStack> {
        updateNeighboringCables()
        
        // TODO: notify something like an EnergyNetworkManager of the destruction of this cable
        
        return super.destroy(dropItems)
    }
    
    override fun saveData() = Unit
    
    override fun handleTick() = Unit
    
    override fun handleRightClick(event: PlayerInteractEvent) = Unit // TODO: Configuration Menu
    
    
}