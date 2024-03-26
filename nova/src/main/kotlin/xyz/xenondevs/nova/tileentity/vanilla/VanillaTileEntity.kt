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
    
    internal abstract val type: Type
    
    internal abstract fun handleInitialized()
    
    internal abstract fun handleRemoved(unload: Boolean)
    
    internal open fun handleBlockUpdate() = Unit
    
    internal open fun saveData() {
        storeData("type", type)
        saveDataAccessors()
    }
    
    internal fun meetsBlockStateRequirement(): Boolean {
        return type.requirement(pos.block)
    }
    
    internal enum class Type(val id: String, val constructor: VanillaTileEntityConstructor, val requirement: (Block) -> Boolean) {
        
        CHEST("minecraft:chest", ::VanillaChestTileEntity, { it.state is Chest }),
        FURNACE("minecraft:furnace", ::VanillaFurnaceTileEntity, { it.state is Furnace }),
        CONTAINER("minecraft:container", ::VanillaContainerTileEntity, { it.state is Container }),
        CAULDRON("minecraft:cauldron", ::VanillaCauldronTileEntity, { it.type.isCauldron() });
        
        companion object {
            
            fun of(block: Block): Type? =
                entries.firstOrNull { it.requirement(block) }
            
        }
        
    }
    
    internal companion object {
        
        fun of(pos: BlockPos, data: Compound): VanillaTileEntity? {
            val id: String = data["id"]!!
            val type = Type.entries.firstOrNull { it.id == id } ?: return null
            if (type.requirement(pos.block)) {
                return type.constructor(pos, data)
            }
            return null
        }
        
    }
    
}