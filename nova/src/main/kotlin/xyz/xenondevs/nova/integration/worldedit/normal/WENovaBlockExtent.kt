package xyz.xenondevs.nova.integration.worldedit.normal

import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.world.block.BlockStateHolder
import xyz.xenondevs.nova.integration.worldedit.NovaBlockExtent

class WENovaBlockExtent(event: EditSessionEvent) : NovaBlockExtent(event) {
    
    override fun <T : BlockStateHolder<T>?> setBlock(location: BlockVector3, block: T): Boolean {
        if (setNovaBlock(location.x, location.y, location.z, block))
            return true
        
        return super.setBlock(location, block)
    }
    
}