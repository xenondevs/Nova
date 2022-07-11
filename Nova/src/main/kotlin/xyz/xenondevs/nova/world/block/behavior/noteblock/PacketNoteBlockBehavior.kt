@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.world.block.behavior.noteblock

import net.minecraft.network.protocol.game.ClientboundBlockEventPacket
import net.minecraft.world.level.block.Blocks
import org.bukkit.Material
import org.bukkit.Note
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundBlockEventPacketEvent
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundSoundPacketEvent
import xyz.xenondevs.nova.tileentity.vanilla.VanillaNoteBlockTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.pos

internal object PacketNoteBlockBehavior : Listener {
    
    fun init() {
        registerPacketListener()
        registerEventsExcept(NotePlayEvent::class)
        registerEventFirst(NotePlayEvent::class)
    }
    
    /**
     * Implements custom right-click behavior of note blocks and stores data inside the [VanillaNoteBlockTileEntity]
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleInteract(event: PlayerInteractEvent) {
        val action = event.action
        
        if (action == Action.RIGHT_CLICK_BLOCK) {
            // allow block placing
            if (event.player.isSneaking && event.item?.type?.isBlock == true)
                return
            
            val block = event.clickedBlock!!
            if (block.type == Material.NOTE_BLOCK) {
                event.isCancelled = true
                
                val pos = block.pos
                val vnb = VanillaTileEntityManager.getTileEntityAt(pos) as? VanillaNoteBlockTileEntity
                if (vnb != null)
                    NoteBlockBehavior.cycleNote(vnb)
            }
        }
    }
    
    /**
     * Changes the values in the [NotePlayEvent] so that other plugins can keep using it.
     */
    @EventHandler
    private fun handleNotePlay(event: NotePlayEvent) {
        val vnb = VanillaTileEntityManager.getTileEntityAt(event.block.pos) as? VanillaNoteBlockTileEntity ?: return
        event.note = Note(vnb.note)
        event.instrument = vnb.instrument.bukkitInstrument
    }
    
    /**
     * Prevents [ClientboundBlockEventPackets][ClientboundBlockEventPacket] for note blocks and replaces them with
     * the proper particle packet.
     */
    @PacketHandler
    private fun handleBlockEvent(event: ClientboundBlockEventPacketEvent) {
        if (event.block == Blocks.NOTE_BLOCK) {
            event.isCancelled = true
            
            val pos = event.pos.toNovaPos(event.player.world)
            val vnb = VanillaTileEntityManager.getTileEntityAt(pos) as? VanillaNoteBlockTileEntity ?: return
            event.player.send(vnb.particlePacket)
        }
    }
    
    /**
     * Corrects note block sounds (type and pitch).
     */
    @PacketHandler
    private fun handleSound(event: ClientboundSoundPacketEvent) {
        val pos = BlockPos(event.player.world, (event.x - 0.5).toInt(), (event.y - 0.5).toInt(), (event.z - 0.5).toInt())
        val vnb = VanillaTileEntityManager.getTileEntityAt(pos) as? VanillaNoteBlockTileEntity ?: return
        
        event.packet = vnb.soundPacket
    }
    
}