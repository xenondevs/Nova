package xyz.xenondevs.nova.data.resources.model.config

import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.world.block.model.NoteBlockModelProvider
import org.bukkit.Instrument as BukkitInstrument

internal data class NoteBlockStateConfig(
    val instrument: Instrument,
    val note: Int,
    val powered: Boolean
) : BlockStateConfig {
    
    override val id = getIdOf(instrument, note, powered)
    override val variantString = "instrument=${instrument.name.lowercase()},note=$note,powered=$powered"
    
    init {
        require(note in 0..24)
    }
    
    companion object : BlockStateConfigType<NoteBlockStateConfig> {
        
        override val maxId = 799
        override val blockedIds = hashSetOf(0)
        override val fileName = "note_block"
        override val modelProvider = NoteBlockModelProvider
        
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
                Instrument.valueOf(properties["instrument"]!!.uppercase()),
                properties["note"]!!.toInt(),
                properties["powered"]!!.toBooleanStrict()
            )
        }
        
    }
    
    enum class Instrument(val bukkitInstrument: BukkitInstrument) {
        BANJO(BukkitInstrument.BANJO),
        BASEDRUM(BukkitInstrument.BASS_DRUM),
        BASS(BukkitInstrument.BASS_GUITAR),
        BELL(BukkitInstrument.BELL),
        BIT(BukkitInstrument.BIT),
        CHIME(BukkitInstrument.CHIME),
        COW_BELL(BukkitInstrument.COW_BELL),
        DIDGERIDOO(BukkitInstrument.DIDGERIDOO),
        FLUTE(BukkitInstrument.FLUTE),
        GUITAR(BukkitInstrument.GUITAR),
        HARP(BukkitInstrument.PIANO),
        HAT(BukkitInstrument.STICKS),
        IRON_XYLOPHONE(BukkitInstrument.IRON_XYLOPHONE),
        PLING(BukkitInstrument.PLING),
        SNARE(BukkitInstrument.SNARE_DRUM),
        XYLOPHONE(BukkitInstrument.XYLOPHONE)
    }
    
}