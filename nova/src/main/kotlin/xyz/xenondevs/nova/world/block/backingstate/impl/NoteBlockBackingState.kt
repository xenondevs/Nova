package xyz.xenondevs.nova.world.block.backingstate.impl

import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.entity.SkullBlockEntity
import org.bukkit.Note
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.NotePlayEvent
import xyz.xenondevs.nmsutils.particle.noteColor
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.resources.model.blockstate.NoteBlockStateConfig
import xyz.xenondevs.nova.tileentity.vanilla.VanillaNoteBlockTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.Instrument
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.backingstate.BackingState
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager
import xyz.xenondevs.nova.world.pos
import kotlin.random.Random

private val PITCH_TABLE: FloatArray = floatArrayOf(
    0.5f, 0.53f, 0.56f, 0.59f, 0.63f, 0.67f, 0.71f, 0.75f, 0.79f, 0.84f,
    0.89f, 0.94f, 1.0f, 1.06f, 1.12f, 1.19f, 1.26f, 1.33f, 1.41f, 1.5f,
    1.59f, 1.68f, 1.78f, 1.89f, 2.0f
)

internal object NoteBlockBackingState : BackingState(NoteBlockStateConfig, false), Listener {
    
    override fun init() {
        registerEvents()
    }
    
    fun cycleNote(vnb: VanillaNoteBlockTileEntity) {
        val instrument = vnb.instrument
        if (instrument.isTunable) {
            vnb.cycleNote()
        }
        
        if (vnb.instrument.worksAboveNoteBlock || vnb.pos.location.add(0.0, 1.0, 0.0).block.type.isAir)
            playNote(vnb)
    }
    
    fun playNote(vnb: VanillaNoteBlockTileEntity) {
        val event = NotePlayEvent(vnb.block, vnb.instrument.bukkitInstrument, Note(vnb.note))
        callEvent(event)
        
        if (event.isCancelled)
            return
        
        if (playSound(vnb) && vnb.instrument.isTunable)
            spawnParticle(vnb)
    }
    
    private fun playSound(vnb: VanillaNoteBlockTileEntity): Boolean {
        val pos = vnb.pos
        val instrument = vnb.instrument
        val soundEvent: Holder<SoundEvent> =
            if (instrument == Instrument.CUSTOM_HEAD) {
                val sound = (pos.world.serverLevel.getBlockEntity(pos.add(0, 1, 0).nmsPos) as? SkullBlockEntity)?.noteBlockSound
                    ?: return false
                Holder.direct(SoundEvent.createVariableRangeEvent(sound))
            } else instrument.soundEvent
        
        pos.world.serverLevel.playSeededSound(
            null,
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5,
            soundEvent,
            SoundSource.RECORDS,
            3f,
            if (instrument.isTunable) PITCH_TABLE[vnb.note] else 1f,
            Random.nextLong()
        )
        
        return true
    }
    
    private fun spawnParticle(vnb: VanillaNoteBlockTileEntity) {
        val packet = particle(ParticleTypes.NOTE, vnb.block.location.add(0.5, 1.0, 0.5)) { noteColor(vnb.note) }
        FakeEntityManager.getChunkViewers(vnb.pos.chunkPos).forEach { it.send(packet) }
    }
    
    /**
     * Handles changes to the note bock data.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handlePhysics(event: BlockPhysicsEvent) {
        val block = event.block
        val pos = block.pos
        
        tryUpdateNoteBlock(pos.add(0, 1, 0))
        tryUpdateNoteBlock(pos.add(0, -1, 0))
    }
    
    private fun tryUpdateNoteBlock(pos: BlockPos) {
        // is this actually a vanilla note block?
        val vnb = VanillaTileEntityManager.getTileEntityAt(pos) as? VanillaNoteBlockTileEntity ?: return
        
        // update instrument
        vnb.instrument = Instrument.determineInstrument(pos)
    }
    
}
