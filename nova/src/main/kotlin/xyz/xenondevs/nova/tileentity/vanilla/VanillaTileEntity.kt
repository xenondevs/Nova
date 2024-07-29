package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.Material
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.world.BlockPos

private typealias VanillaTileEntityConstructor = (BlockPos, Compound) -> VanillaTileEntity

internal abstract class VanillaTileEntity internal constructor(
    val pos: BlockPos,
    override val data: Compound
) : DataHolder(false) {
    
    val isChunkLoaded
        get() = pos.chunkPos.isLoaded()
    
    abstract val type: Type
    
    open fun handleEnable() = Unit
    open fun handleDisable() = Unit
    open fun handlePlace() = Unit
    open fun handleBreak() = Unit
    open fun handleBlockUpdate() = Unit
    
    open fun saveData() {
        storeData("type", type)
        saveDataAccessors()
    }
    
    enum class Type(val constructor: VanillaTileEntityConstructor, val materials: Set<Material>) {
        
        CHEST(::VanillaChestTileEntity, setOf(Material.CHEST, Material.TRAPPED_CHEST)),
        FURNACE(::VanillaFurnaceTileEntity, setOf(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER)),
        CONTAINER(::VanillaContainerTileEntity, setOf(Material.BARREL, Material.DISPENSER, Material.DROPPER, Material.HOPPER, Material.SHULKER_BOX)),
        CAULDRON(::VanillaCauldronTileEntity, setOf(Material.CAULDRON));
        // TODO: brewing stand, needs legacy conversion from container
        // TODO: crafter
        
        companion object {
            
            private val map: Map<Material, Type> = run {
                val map = HashMap<Material, Type>()
                for (entry in entries) {
                    for (material in entry.materials) {
                        map[material] = entry
                    }
                }
                map
            }
            
            fun of(block: Material): Type? =
                map[block]
            
        }
        
    }
    
}