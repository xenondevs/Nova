package xyz.xenondevs.nova.integration.worldedit

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.extent.AbstractDelegateExtent
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockStateHolder
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.world.BlockPos

abstract class NovaBlockExtent(private val event: EditSessionEvent) : AbstractDelegateExtent(event.extent) {
    
    fun <T : BlockStateHolder<T>?> setNovaBlock(x: Int, y: Int, z: Int, block: T): Boolean {
        val novaId = (block as? BaseBlock)?.nbtData?.getString("nova")
        if (novaId != null) {
            val pos = BlockPos(BukkitAdapter.adapt(event.world), x, y, z)
            WorldDataManager.addOrphanBlock(pos, NovaRegistries.BLOCK[novaId]!!)
            
            return true
        }
        
        return false
    }
    
}