package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.NoteBlock
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.util.Instrument
import xyz.xenondevs.nova.util.intValue

// The base of the number system - i.e. how many values each property can have
private const val NOTE_BASE = 25
private const val INSTRUMENT_BASE = 23
private const val POWERED_BASE = 2

internal data class NoteBackingStateConfig(
    val instrument: Instrument,
    val note: Int,
    val powered: Boolean
) : BackingStateConfig() {
    
    override val type = NoteBackingStateConfig
    override val id = getIdOf(instrument, note, powered)
    override val variantString = "instrument=${instrument.name.lowercase()},note=$note,powered=$powered"
    override val vanillaBlockState: BlockState = Blocks.NOTE_BLOCK.defaultBlockState()
        .setValue(NoteBlock.INSTRUMENT, instrument.nmsInstrument)
        .setValue(NoteBlock.NOTE, note)
        .setValue(NoteBlock.POWERED, powered)
    
    init {
        require(note in 0..24)
    }
    
    companion object : DynamicDefaultingBackingStateConfigType<NoteBackingStateConfig>(1149, "note_block") {
        
        fun getIdOf(instrument: Instrument, note: Int, powered: Boolean): Int {
            return instrument.ordinal * NOTE_BASE * POWERED_BASE + note * POWERED_BASE + powered.intValue
        }
        
        override fun of(id: Int): NoteBackingStateConfig {
            return NoteBackingStateConfig(
                 Instrument.entries[id / POWERED_BASE / NOTE_BASE % INSTRUMENT_BASE],
                id / POWERED_BASE % NOTE_BASE,
                id % POWERED_BASE == 1
            )
        }
        
        override fun of(properties: Map<String, String>): NoteBackingStateConfig {
            return NoteBackingStateConfig(
                properties["instrument"]?.let { Instrument.valueOf(it.uppercase()) } ?: Instrument.HARP,
                properties["note"]?.toInt() ?: 0,
                properties["powered"]?.toBoolean() ?: false
            )
        }
        
    }
    
}