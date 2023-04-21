package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.Furnace
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.util.item.isCauldron

private typealias VanillaTileEntityConstructor = (VanillaTileEntityState) -> VanillaTileEntity

internal abstract class VanillaTileEntity internal constructor(val blockState: VanillaTileEntityState) : DataHolder(false) {
    
    override val data = blockState.data
    val pos = blockState.pos
    val block = pos.block
    
    val isChunkLoaded
        get() = pos.chunkPos.isLoaded()
    
    internal abstract val type: Type
    
    internal abstract fun handleInitialized()
    
    internal abstract fun handleRemoved(unload: Boolean)
    
    internal open fun handleBlockUpdate() = Unit
    
    internal open fun saveData() {
        saveDataAccessors()
    }
    
    internal fun meetsBlockStateRequirement(): Boolean {
        return type.requirement(blockState.pos.block)
    }
    
    internal enum class Type(val id: String, val constructor: VanillaTileEntityConstructor, val requirement: (Block) -> Boolean) {
        
        CHEST("minecraft:chest", ::VanillaChestTileEntity, { it.state is Chest }),
        FURNACE("minecraft:furnace", ::VanillaFurnaceTileEntity, { it.state is Furnace }),
        CONTAINER("minecraft:container", ::VanillaContainerTileEntity, { it.state is Container }),
        CAULDRON("minecraft:cauldron", ::VanillaCauldronTileEntity, { it.type.isCauldron() }),
        NOTE_BLOCK("minecraft:note_block", ::VanillaNoteBlockTileEntity, { it.type == Material.NOTE_BLOCK }),
        DAYLIGHT_DETECTOR("minecraft:daylight_detector", ::VanillaDaylightDetectorTileEntity, { it.type == Material.DAYLIGHT_DETECTOR });
        
        companion object {
            
            fun of(block: Block): Type? =
                Type.values().firstOrNull { it.requirement(block) }
            
        }
        
    }
    
    internal companion object {
        
        fun of(blockState: VanillaTileEntityState): VanillaTileEntity? {
            val type = Type.values().firstOrNull { it.id == blockState.id.toString() } ?: return null
            if (type.requirement(blockState.pos.block)) {
                return type.constructor(blockState)
            }
            return null
        }
        
    }
    
}