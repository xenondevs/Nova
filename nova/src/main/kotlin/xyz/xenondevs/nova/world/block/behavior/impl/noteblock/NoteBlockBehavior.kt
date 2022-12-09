package xyz.xenondevs.nova.world.block.behavior.impl.noteblock

import org.bukkit.Note
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.NotePlayEvent
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.resources.model.blockstate.NoteBlockStateConfig
import xyz.xenondevs.nova.tileentity.vanilla.VanillaNoteBlockTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.Instrument
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager
import xyz.xenondevs.nova.world.pos
import org.bukkit.Instrument as BukkitInstrument

internal object NoteBlockBehavior : BlockBehavior(NoteBlockStateConfig, false), Listener {
    
    override fun init() {
        registerEvents()
        
        if (!DEFAULT_CONFIG.getBoolean("use_agent"))
            PacketNoteBlockBehavior.init()
    }
    
    fun cycleNote(vnb: VanillaNoteBlockTileEntity) {
        vnb.cycleNote()
        
        if (vnb.pos.location.add(0.0, 1.0, 0.0).block.type.isAir)
            playNote(vnb)
    }
    
    fun playNote(vnb: VanillaNoteBlockTileEntity) {
        val pos = vnb.pos
        
        val event = NotePlayEvent(vnb.block, vnb.instrument.bukkitInstrument ?: BukkitInstrument.PIANO, Note(vnb.note))
        callEvent(event)
        
        if (event.isCancelled)
            return
        
        val soundPacket = vnb.soundPacket
        val particlePacket = vnb.particlePacket
        
        FakeEntityManager.getChunkViewers(pos.chunkPos).forEach {
            it.send(soundPacket, particlePacket)
        }
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
        vnb.instrument = Instrument.byBlockAbove(pos.add(0, 1, 0)) ?: Instrument.byBlockBelow(pos.add(0, -1, 0))
    }
    
}
