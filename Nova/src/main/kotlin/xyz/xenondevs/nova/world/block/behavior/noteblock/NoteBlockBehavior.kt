package xyz.xenondevs.nova.world.block.behavior.noteblock

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.bukkit.Material
import org.bukkit.Note
import org.bukkit.block.data.type.NoteBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.NotePlayEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.resources.model.config.NoteBlockStateConfig
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.tileentity.vanilla.VanillaNoteBlockTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager
import xyz.xenondevs.nova.world.pos
import net.minecraft.world.level.block.Block as MojangBlock

internal object NoteBlockBehavior : Initializable(), Listener {
    
    val DEFAULT_STATE_CONFIG = NoteBlockStateConfig.of(0) // TODO
    
    val DEFAULT_STATE: BlockState = MojangBlock.BLOCK_STATE_REGISTRY.first {
        it.`is`(Blocks.NOTE_BLOCK)
            && it.hasProperty(BlockStateProperties.NOTEBLOCK_INSTRUMENT, DEFAULT_STATE_CONFIG.instrument.nmsInstrument)
            && it.hasProperty(BlockStateProperties.NOTE, DEFAULT_STATE_CONFIG.note)
            && it.hasProperty(BlockStateProperties.POWERED, DEFAULT_STATE_CONFIG.powered)
    }
    
    override val inMainThread = true
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
        LOGGER.info("Initializing NoteBlockBehavior")
        
        registerEvents()
        
        if (DEFAULT_CONFIG.getBoolean("use_agent")) {
            AgentNoteBlockBehavior.init()
        } else {
            PacketNoteBlockBehavior.init()
        }
    }
    
    fun cycleNote(vnb: VanillaNoteBlockTileEntity) {
        vnb.cycleNote()
        
        if (vnb.pos.location.add(0.0, 1.0, 0.0).block.type.isAir)
            playNote(vnb)
    }
    
    fun playNote(vnb: VanillaNoteBlockTileEntity) {
        val pos = vnb.pos
        
        val event = NotePlayEvent(vnb.block, vnb.instrument.bukkitInstrument, Note(vnb.note))
        callEvent(event)
        
        if (event.isCancelled)
            return
        
        val soundPacket = vnb.soundPacket
        val particlePacket = vnb.particlePacket
        
        FakeArmorStandManager.getChunkViewers(pos.chunkPos).forEach {
            it.send(soundPacket, particlePacket)
        }
    }
    
    /**
     * Handles changes to the note bock data.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handlePhysics(event: BlockPhysicsEvent) {
        val block = event.block
        val pos = block.pos
        val changed = event.changed
        
        if (changed is NoteBlock) {
            // set to default state (cancelling does not work)
            pos.world.serverLevel.setBlock(pos.nmsPos, DEFAULT_STATE, 1024)
        } else {
            val above = pos.copy(y = pos.y + 1)
            if (above.block.type == Material.NOTE_BLOCK) {
                // is this actually a vanilla note block?
                val vnb = VanillaTileEntityManager.getTileEntityAt(above) as? VanillaNoteBlockTileEntity ?: return
                
                // update instrument
                vnb.instrument = Instrument.byBlockType(pos)
            }
        }
    }
    
}
