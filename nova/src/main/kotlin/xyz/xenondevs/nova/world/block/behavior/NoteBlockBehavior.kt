package xyz.xenondevs.nova.world.block.behavior

import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.entity.SkullBlockEntity
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Note
import org.bukkit.Tag
import org.bukkit.block.BlockFace
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockInteract
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.util.Instrument
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.particle.noteColor
import xyz.xenondevs.nova.util.particle.particle
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.NOTE_BLOCK_INSTRUMENT
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.NOTE_BLOCK_NOTE
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.POWERED
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager
import xyz.xenondevs.nova.world.format.WorldDataManager
import kotlin.random.Random

private val PITCH_TABLE: FloatArray = floatArrayOf(
    0.5f, 0.53f, 0.56f, 0.59f, 0.63f, 0.67f, 0.71f, 0.75f, 0.79f, 0.84f,
    0.89f, 0.94f, 1.0f, 1.06f, 1.12f, 1.19f, 1.26f, 1.33f, 1.41f, 1.5f,
    1.59f, 1.68f, 1.78f, 1.89f, 2.0f
)

internal object NoteBlockBehavior : BlockBehavior {
    
    override fun handleInteract(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): Boolean {
        val clickedFace = ctx[DefaultContextParamTypes.CLICKED_BLOCK_FACE]
        val item = ctx[DefaultContextParamTypes.INTERACTION_ITEM_STACK]
        
        if (item != null && item.novaItem == null && Tag.ITEMS_NOTE_BLOCK_TOP_INSTRUMENTS.isTagged(item.type) && clickedFace == BlockFace.UP)
            return false
        
        cycleNote(pos, state)
        playNote(pos, state)
        
        return true
    }
    
    override fun handleAttack(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) {
        playNote(pos, state)
    }
    
    override fun handleNeighborChanged(pos: BlockPos, state: NovaBlockState, neighborPos: BlockPos) {
        val poweredPreviously = state.getOrThrow(POWERED)
        val poweredNow = pos.block.isBlockIndirectlyPowered
        
        val newState = state
            .with(POWERED, poweredNow)
            .with(NOTE_BLOCK_INSTRUMENT, Instrument.determineInstrument(pos))
        
        if (!poweredPreviously && poweredNow)
            playNote(pos, newState)
        
        if (newState != state) {
            WorldDataManager.setBlockState(pos, newState)
            
            // We need to still cause block updates even though the block wasn't changed to keep vanilla behavior
            val level = pos.world.serverLevel
            val nmsPos = pos.nmsPos
            val nmsState = pos.nmsBlockState
            level.notifyAndUpdatePhysics(nmsPos, level.getChunkAt(nmsPos), nmsState, nmsState, nmsState, 3, 512)
        }
    }
    
    private fun cycleNote(pos: BlockPos, state: NovaBlockState) {
        WorldDataManager.setBlockState(pos, state.cycle(NOTE_BLOCK_NOTE))
    }
    
    private fun playNote(pos: BlockPos, state: NovaBlockState) {
        val instrument = state.getOrThrow(NOTE_BLOCK_INSTRUMENT)
        val note = state.getOrThrow(NOTE_BLOCK_NOTE)
        
        if (!instrument.worksAboveNoteBlock && !pos.add(0, 1, 0).nmsBlockState.isAir)
            return
        
        val event = NotePlayEvent(pos.block, instrument.bukkitInstrument, Note(note))
        callEvent(event)
        
        if (event.isCancelled)
            return
        
        if (playSound(pos, instrument, note) && instrument.isTunable)
            spawnParticle(pos, note)
        
    }
    
    private fun playSound(pos: BlockPos, instrument: Instrument, note: Int): Boolean {
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
            if (instrument.isTunable) PITCH_TABLE[note] else 1f,
            Random.nextLong()
        )
        
        return true
    }
    
    private fun spawnParticle(pos: BlockPos, note: Int) {
        val packet = particle(ParticleTypes.NOTE, pos.block.location.add(0.5, 1.0, 0.5)) { noteColor(note) }
        FakeEntityManager.getChunkViewers(pos.chunkPos).forEach { it.send(packet) }
    }
    
    override fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> {
        if (ctx[DefaultContextParamTypes.SOURCE_PLAYER]?.gameMode == GameMode.CREATIVE)
            return emptyList()
        
        return listOf(ItemStack(Material.NOTE_BLOCK))
    }
    
}