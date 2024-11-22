package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.NoteBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import xyz.xenondevs.nova.util.intValue

// The base of the number system - i.e. how many values each property can have
private const val NOTE_BASE = 25
private const val INSTRUMENT_BASE = 23
private const val POWERED_BASE = 2

internal data class NoteBackingStateConfig(
    val instrument: NoteBlockInstrument,
    val note: Int,
    val powered: Boolean
) : BackingStateConfig() {
    
    override val type = NoteBackingStateConfig
    override val id = getIdOf(instrument, note, powered)
    override val waterlogged = false
    override val variantString = "instrument=${instrument.name.lowercase()},note=$note,powered=$powered"
    override val vanillaBlockState: BlockState = Blocks.NOTE_BLOCK.defaultBlockState()
        .setValue(NoteBlock.INSTRUMENT, instrument)
        .setValue(NoteBlock.NOTE, note)
        .setValue(NoteBlock.POWERED, powered)
    
    init {
        require(note in 0..24)
    }
    
    companion object : DynamicDefaultingBackingStateConfigType<NoteBackingStateConfig>(1149, "note_block") {
        
        override val properties = hashSetOf("instrument", "note", "powered")
        
        fun getIdOf(instrument: NoteBlockInstrument, note: Int, powered: Boolean): Int {
            return instrument.ordinal * NOTE_BASE * POWERED_BASE + note * POWERED_BASE + powered.intValue
        }
        
        override fun of(id: Int, waterlogged: Boolean): NoteBackingStateConfig {
            if (waterlogged)
                throw UnsupportedOperationException("Note block cannot be waterlogged")
            
            return NoteBackingStateConfig(
                NoteBlockInstrument.entries[id / POWERED_BASE / NOTE_BASE % INSTRUMENT_BASE],
                id / POWERED_BASE % NOTE_BASE,
                id % POWERED_BASE == 1
            )
        }
        
        override fun of(properties: Map<String, String>): NoteBackingStateConfig {
            return NoteBackingStateConfig(
                properties["instrument"]?.let { NoteBlockInstrument.valueOf(it.uppercase()) } ?: NoteBlockInstrument.HARP,
                properties["note"]?.toInt() ?: 0,
                properties["powered"]?.toBoolean() ?: false
            )
        }
        
    }
    
}