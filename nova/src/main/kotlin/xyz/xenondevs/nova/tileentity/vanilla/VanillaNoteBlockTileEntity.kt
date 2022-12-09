package xyz.xenondevs.nova.tileentity.vanilla

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundSource
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.util.Instrument
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.particle.ParticleEffect
import xyz.xenondevs.particle.data.color.NoteColor

private val PITCH_TABLE: FloatArray = floatArrayOf(
    0.5f, 0.53f, 0.56f, 0.59f, 0.63f, 0.67f, 0.71f, 0.75f, 0.79f, 0.84f,
    0.89f, 0.94f, 1.0f, 1.06f, 1.12f, 1.19f, 1.26f, 1.33f, 1.41f, 1.5f,
    1.59f, 1.68f, 1.78f, 1.89f, 2.0f
)

internal class VanillaNoteBlockTileEntity constructor(blockState: VanillaTileEntityState) : VanillaTileEntity(blockState) {
    
    override val type = Type.NOTE_BLOCK
    
    var note: Int = retrieveData("note") { 0 }
        set(value) {
            if (field != value) {
                field = value
                
                _particlePacket = null
                _soundPacket = null
            }
        }
    
    var instrument: Instrument = retrieveData("instrument") { Instrument.byBlockAbove(pos.add(0, 1, 0)) ?: Instrument.byBlockBelow(pos.add(0, -1, 0)) }
        set(value) {
            if (field != value) {
                field = value
                
                _particlePacket = null
                _soundPacket = null
            }
        }
    
    var powered: Boolean = retrieveData("powered") { false }
    
    private var _particlePacket: ClientboundLevelParticlesPacket? = null
    val particlePacket: ClientboundLevelParticlesPacket
        get() {
            if (_particlePacket == null)
                updateParticlePacket()
            
            return _particlePacket!!
        }
    
    private var _soundPacket: ClientboundSoundPacket? = null
    val soundPacket: ClientboundSoundPacket
        get() {
            if (_soundPacket == null)
                updateSoundPacket()
            
            return _soundPacket!!
        }
    
    private fun updateParticlePacket() {
        _particlePacket = particleBuilder(ParticleEffect.NOTE) {
            location(block.location.add(0.5, 1.0, 0.5))
            data(NoteColor(note))
        }.packet as ClientboundLevelParticlesPacket
    }
    
    private fun updateSoundPacket() {
        // TODO: Add support for custom player head sounds
        _soundPacket = ClientboundSoundPacket(
            instrument.soundEvent,
            SoundSource.RECORDS,
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5,
            3.0f,
            PITCH_TABLE[note],
            0L
        )
    }
    
    fun cycleNote() {
        note = (note + 1) % 25
    }
    
    override fun saveData() {
        storeData("note", note)
        storeData("instrument", instrument)
        storeData("powered", powered)
    }
    
    override fun handleInitialized() = Unit
    override fun handleRemoved(unload: Boolean) = Unit
    
}