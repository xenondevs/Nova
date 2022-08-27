package xyz.xenondevs.nova.util

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import xyz.xenondevs.nova.world.BlockPos
import org.bukkit.Instrument as BukkitInstrument

enum class Instrument(val bukkitInstrument: BukkitInstrument, val nmsInstrument: NoteBlockInstrument) {
    
    HARP(BukkitInstrument.PIANO, NoteBlockInstrument.HARP),
    BASEDRUM(BukkitInstrument.BASS_DRUM, NoteBlockInstrument.BASEDRUM),
    SNARE(BukkitInstrument.SNARE_DRUM, NoteBlockInstrument.SNARE),
    HAT(BukkitInstrument.STICKS, NoteBlockInstrument.HAT),
    BASS(BukkitInstrument.BASS_GUITAR, NoteBlockInstrument.BASS),
    FLUTE(BukkitInstrument.FLUTE, NoteBlockInstrument.FLUTE),
    BELL(BukkitInstrument.BELL, NoteBlockInstrument.BELL),
    GUITAR(BukkitInstrument.GUITAR, NoteBlockInstrument.GUITAR),
    CHIME(BukkitInstrument.CHIME, NoteBlockInstrument.CHIME),
    XYLOPHONE(BukkitInstrument.XYLOPHONE, NoteBlockInstrument.XYLOPHONE),
    IRON_XYLOPHONE(BukkitInstrument.IRON_XYLOPHONE, NoteBlockInstrument.IRON_XYLOPHONE),
    COW_BELL(BukkitInstrument.COW_BELL, NoteBlockInstrument.COW_BELL),
    DIDGERIDOO(BukkitInstrument.DIDGERIDOO, NoteBlockInstrument.DIDGERIDOO),
    BIT(BukkitInstrument.BIT, NoteBlockInstrument.BIT),
    BANJO(BukkitInstrument.BANJO, NoteBlockInstrument.BANJO),
    PLING(BukkitInstrument.PLING, NoteBlockInstrument.PLING);
    
    companion object {
        
        fun byBlockType(pos: BlockPos): Instrument {
            return NoteBlockInstrument.byState(pos.world.serverLevel.getBlockState(pos.nmsPos)).instrument
        }
        
    }
    
}

internal val BukkitInstrument.instrument: Instrument
    get() = when(this) {
        BukkitInstrument.PIANO -> Instrument.HARP
        BukkitInstrument.BASS_DRUM -> Instrument.BASEDRUM
        BukkitInstrument.SNARE_DRUM -> Instrument.SNARE
        BukkitInstrument.STICKS -> Instrument.HAT
        BukkitInstrument.BASS_GUITAR -> Instrument.BASS
        BukkitInstrument.FLUTE -> Instrument.FLUTE
        BukkitInstrument.BELL -> Instrument.BELL
        BukkitInstrument.GUITAR -> Instrument.GUITAR
        BukkitInstrument.CHIME -> Instrument.CHIME
        BukkitInstrument.XYLOPHONE -> Instrument.XYLOPHONE
        BukkitInstrument.IRON_XYLOPHONE -> Instrument.IRON_XYLOPHONE
        BukkitInstrument.COW_BELL -> Instrument.COW_BELL
        BukkitInstrument.DIDGERIDOO -> Instrument.DIDGERIDOO
        BukkitInstrument.BIT -> Instrument.BIT
        BukkitInstrument.BANJO -> Instrument.BANJO
        BukkitInstrument.PLING -> Instrument.PLING
    }

internal val NoteBlockInstrument.instrument: Instrument
    get() = when(this) {
        NoteBlockInstrument.HARP -> Instrument.HARP
        NoteBlockInstrument.BASEDRUM -> Instrument.BASEDRUM
        NoteBlockInstrument.SNARE -> Instrument.SNARE
        NoteBlockInstrument.HAT -> Instrument.HAT
        NoteBlockInstrument.BASS -> Instrument.BASS
        NoteBlockInstrument.FLUTE -> Instrument.FLUTE
        NoteBlockInstrument.BELL -> Instrument.BELL
        NoteBlockInstrument.GUITAR -> Instrument.GUITAR
        NoteBlockInstrument.CHIME -> Instrument.CHIME
        NoteBlockInstrument.XYLOPHONE -> Instrument.XYLOPHONE
        NoteBlockInstrument.IRON_XYLOPHONE -> Instrument.IRON_XYLOPHONE
        NoteBlockInstrument.COW_BELL -> Instrument.COW_BELL
        NoteBlockInstrument.DIDGERIDOO -> Instrument.DIDGERIDOO
        NoteBlockInstrument.BIT -> Instrument.BIT
        NoteBlockInstrument.BANJO -> Instrument.BANJO
        NoteBlockInstrument.PLING -> Instrument.PLING
    }