package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.Furnace
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.util.item.isCauldron

private typealias VanillaTileEntityConstructor = (VanillaTileEntityState) -> VanillaTileEntity

abstract class VanillaTileEntity internal constructor(val blockState: VanillaTileEntityState) : DataHolder(false) {
    
    override val data = blockState.data
    val block = blockState.pos.block
    
    internal abstract fun handleRemoved(unload: Boolean)
    
    internal abstract fun handleInitialized()
    
    internal abstract fun saveData()
    
    internal open fun handleBlockUpdate() = Unit
    
    internal enum class Type(val id: String, val constructor: VanillaTileEntityConstructor) {
    
        CHEST("minecraft:chest", ::VanillaChestTileEntity),
        FURNACE("minecraft:furnace", ::VanillaFurnaceTileEntity),
        CONTAINER("minecraft:container", ::VanillaContainerTileEntity),
        CAULDRON("minecraft:cauldron", ::VanillaCauldronTileEntity);
        
        companion object {
            fun of(block: Block): Type? {
                val state = block.state
                val type = block.type
                return when {
                    state is Chest -> CHEST
                    state is Furnace -> FURNACE
                    state is Container -> CONTAINER
                    type.isCauldron() -> CAULDRON
                    else -> null
                }
            }
        }
        
    }
    
    internal companion object {
        fun of(blockState: VanillaTileEntityState): VanillaTileEntity? =
            Type.values().firstOrNull { it.id == blockState.id }?.constructor?.invoke(blockState)
    }
    
}