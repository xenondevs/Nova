package xyz.xenondevs.nova.data.resources.model.blockstate

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.NoteBlock
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Material
import xyz.xenondevs.nova.util.Instrument
import xyz.xenondevs.nova.util.intValue

internal data class NoteBlockStateConfig(
    val instrument: Instrument,
    val note: Int,
    val powered: Boolean
) : BlockStateConfig {
    
    override val type = NoteBlockStateConfig
    override val id = getIdOf(instrument, note, powered)
    override val variantString = "instrument=${instrument.name.lowercase()},note=$note,powered=$powered"
    override val blockState: BlockState = Blocks.NOTE_BLOCK.defaultBlockState()
        .setValue(NoteBlock.INSTRUMENT, instrument.nmsInstrument)
        .setValue(NoteBlock.NOTE, note)
        .setValue(NoteBlock.POWERED, powered)
    
    init {
        require(note in 0..24)
    }
    
    companion object : DynamicDefaultingBlockStateConfigType<NoteBlockStateConfig>() {
        
        override val maxId = 799
        override val fileName = "note_block"
        override val material = Material.NOTE_BLOCK
        
        fun getIdOf(instrument: Instrument, note: Int, powered: Boolean): Int {
            return (note shl 5) or (instrument.ordinal shl 1) or powered.intValue
        }
        
        override fun of(id: Int): NoteBlockStateConfig {
            return NoteBlockStateConfig(
                Instrument.values()[id shr 1 and 0xF],
                id shr 5,
                id and 1 == 1,
            )
        }
        
        override fun of(variantString: String): NoteBlockStateConfig {
            val properties = variantString.split(',')
                .associate { val s = it.split('='); s[0] to s[1] }
            
            return NoteBlockStateConfig(
                properties["instrument"]?.let { Instrument.valueOf(it.uppercase()) } ?: Instrument.HARP,
                properties["note"]?.toInt() ?: 0,
                properties["powered"]?.toBoolean() ?: false
            )
        }
        
    }
    
}