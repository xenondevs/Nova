package xyz.xenondevs.nova.world.block.model

import org.bukkit.Material
import org.bukkit.Note
import org.bukkit.block.data.type.NoteBlock
import xyz.xenondevs.nova.data.resources.model.config.NoteBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.data.SolidBlockModelData
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState

@Suppress("UNCHECKED_CAST")
internal class NoteBlockModelProvider(val blockState: NovaBlockState) : BlockModelProvider {
    
    private val pos = blockState.pos
    private val material = blockState.material
    private val modelData = material.block as SolidBlockModelData<NoteBlockStateConfig>
    
    override fun load(placed: Boolean) {
        if (placed) update(0)
    }
    
    override fun remove(broken: Boolean) {
        if (broken) pos.block.type = Material.AIR
    }
    
    override fun update(subId: Int) {
        val block = pos.block
        
        val directional = blockState.getProperty(Directional::class)
        val config = if (directional != null)
            modelData[directional.facing, subId]
        else modelData[subId]
        
        block.type = Material.NOTE_BLOCK
        
        val noteBlock = block.blockData as NoteBlock
        noteBlock.instrument = config.instrument.bukkitInstrument
        noteBlock.note = Note(config.note)
        noteBlock.isPowered = config.powered
        
        block.setBlockData(noteBlock, false)
    }
    
    companion object : BlockModelProviderType<NoteBlockModelProvider> {
        override fun create(blockState: NovaBlockState) = NoteBlockModelProvider(blockState)
    }
    
}