package xyz.xenondevs.nova.tileentity.network.item.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.vanilla.ItemStorageVanillaTileEntity
import xyz.xenondevs.nova.util.CUBE_FACES
import java.util.*

abstract class VanillaItemHolder(
    final override val endPoint: ItemStorageVanillaTileEntity
) : ItemHolder {
    
    override val itemConfig: MutableMap<BlockFace, ItemConnectionType> =
        endPoint.retrieveDoubleEnumMap("itemConfig") { CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { ItemConnectionType.BUFFER } }
    
    override fun saveData() {
        endPoint.storeEnumMap("itemConfig", itemConfig)
    }
    
}

class StaticVanillaItemHolder(
    endPoint: ItemStorageVanillaTileEntity,
    override val inventories: MutableMap<BlockFace, NetworkedInventory>
) : VanillaItemHolder(endPoint)

class DynamicVanillaItemHolder(
    endPoint: ItemStorageVanillaTileEntity,
    val inventoriesGetter: () -> MutableMap<BlockFace, NetworkedInventory>
) : VanillaItemHolder(endPoint) {
    
    override val inventories: MutableMap<BlockFace, NetworkedInventory>
        get() = inventoriesGetter()
    
}
