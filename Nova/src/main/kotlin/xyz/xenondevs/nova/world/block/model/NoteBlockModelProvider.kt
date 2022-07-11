package xyz.xenondevs.nova.world.block.model

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Material
import xyz.xenondevs.nova.data.resources.model.config.NoteBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.data.SolidBlockModelData
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.util.setBlockStateSilently

@Suppress("UNCHECKED_CAST")
internal class NoteBlockModelProvider(val blockState: NovaBlockState) : SolidBlockModelProvider {
    
    private val pos = blockState.pos
    private val material = blockState.material
    private val modelData = material.block as SolidBlockModelData<NoteBlockStateConfig>
    
    override var currentSubId = 0
        private set
    override var currentBlockState = getBlockState(0)
        private set
    
    override fun load(placed: Boolean) {
        if (placed) update(0)
    }
    
    override fun remove(broken: Boolean) {
        if (broken) pos.block.type = Material.AIR
    }
    
    override fun update(subId: Int) {
        if (currentSubId != subId) {
            currentSubId = subId
            currentBlockState = getBlockState(subId)
        }
        
        pos.setBlockStateSilently(currentBlockState)
    }
    
    private fun getBlockState(subId: Int): BlockState {
        val directional = blockState.getProperty(Directional::class)
        val config = if (directional != null)
            modelData[directional.facing, subId]
        else modelData[subId]
    
        return config.blockState
    }
    
    companion object : BlockModelProviderType<NoteBlockModelProvider> {
        override fun create(blockState: NovaBlockState) = NoteBlockModelProvider(blockState)
    }
    
}