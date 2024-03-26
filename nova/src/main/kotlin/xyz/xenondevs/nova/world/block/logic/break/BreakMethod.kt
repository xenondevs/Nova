package xyz.xenondevs.nova.world.block.logic.`break`

import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.item.DefaultBlockOverlays
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.util.broadcastDestructionStage
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import xyz.xenondevs.nova.world.pos
import kotlin.random.Random

internal interface BreakMethod {
    
    val hasClientsidePrediction: Boolean
    var breakStage: Int
    fun stop()
    
    companion object {
        
        private val INVISIBLE = object : BreakMethod {
            override val hasClientsidePrediction = false
            override var breakStage: Int = 0
            override fun stop() {}
        }
        
        fun of(block: Block, material: NovaBlock, entityId: Int = Random.nextInt()): BreakMethod =
            of(block, material, null, entityId)
        
        fun of(
            block: Block,
            material: NovaBlock,
            predictionPlayer: Player?,
            entityId: Int = predictionPlayer?.entityId ?: Random.nextInt()
        ): BreakMethod {
            return if (material.options.showBreakAnimation)
                if (block.type == Material.BARRIER) DisplayEntityBreakMethod(block.pos)
                else PacketBreakMethod(block.pos, entityId, predictionPlayer)
            else INVISIBLE
        }
        
    }
    
}

internal abstract class VisibleBreakMethod(val pos: BlockPos, val predictionPlayer: Player? = null) : BreakMethod {
    override val hasClientsidePrediction = predictionPlayer != null
    val block = pos.block
}

internal class PacketBreakMethod(pos: BlockPos, private val entityId: Int = Random.nextInt(), predictionPlayer: Player? = null) : VisibleBreakMethod(pos, predictionPlayer) {
    
    constructor(pos: BlockPos, predictionPlayer: Player) : this(pos, predictionPlayer.entityId, predictionPlayer)
    
    override var breakStage: Int = -1
        set(stage) {
            if (field == stage) return
            
            field = stage
            
            sendBreakStage(stage)
        }
    
    override fun stop() {
        sendBreakStage(-1)
    }
    
    private fun sendBreakStage(stage: Int) {
        if (predictionPlayer != null) {
            block.broadcastDestructionStage(predictionPlayer, stage)
        } else {
            block.broadcastDestructionStage(entityId, stage)
        }
    }
    
}

internal class DisplayEntityBreakMethod(pos: BlockPos) : VisibleBreakMethod(pos) {
    
    private val itemDisplay = FakeItemDisplay(pos.location.add(.5, .5, .5), true) { _, data ->
        data.itemDisplay = ItemDisplayContext.HEAD
    }
    
    override var breakStage: Int = -1
        set(stage) {
            if (field == stage) return
            
            field = stage
            itemDisplay.updateEntityData(true) {
                itemStack = if (stage in 0..9)
                    DefaultBlockOverlays.BREAK_STAGE_OVERLAY.model.unnamedClientsideProviders[stage].get().nmsCopy
                else ItemStack.EMPTY
            }
        }
    
    override fun stop() {
        itemDisplay.remove()
    }
    
}