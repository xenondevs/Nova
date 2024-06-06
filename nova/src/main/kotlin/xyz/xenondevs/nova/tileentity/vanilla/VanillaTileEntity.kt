package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.Furnace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.util.item.isCauldron
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
    
    fun meetsBlockStateRequirement(): Boolean {
        return type.requirement(pos.block)
    }
    
    enum class Type(val constructor: VanillaTileEntityConstructor, val requirement: (Block) -> Boolean) {
        
        CHEST(::VanillaChestTileEntity, { it.state is Chest }),
        FURNACE(::VanillaFurnaceTileEntity, { it.state is Furnace }),
        CONTAINER(::VanillaContainerTileEntity, { it.state is Container }),
        CAULDRON(::VanillaCauldronTileEntity, { it.type.isCauldron() });
        
        companion object {
            
            fun of(block: Block): Type? =
                entries.firstOrNull { it.requirement(block) }
            
        }
        
    }
    
}