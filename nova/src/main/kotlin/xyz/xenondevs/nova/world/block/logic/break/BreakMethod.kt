package xyz.xenondevs.nova.world.block.logic.`break`

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData.customModelData
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.packetentity.packetItemDisplay
import xyz.xenondevs.nova.util.broadcastDestructionStage
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.item.DefaultBlockOverlays
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
            type: NovaBlock,
            predictionPlayer: Player?,
            entityId: Int = predictionPlayer?.entityId ?: Random.nextInt()
        ): BreakMethod {
            return if (type.getBehaviorOrThrow<Breakable>().showBreakAnimation)
                if (block.type == Material.BARRIER) DisplayEntityBreakMethod(block)
                else PacketBreakMethod(block, entityId, predictionPlayer)
            else INVISIBLE
        }
        
    }
    
}

internal abstract class VisibleBreakMethod(val block: Block, val predictionPlayer: Player? = null) : BreakMethod {
    override val hasClientsidePrediction = predictionPlayer != null
}

internal class PacketBreakMethod(block: Block, private val entityId: Int = Random.nextInt(), predictionPlayer: Player? = null) : VisibleBreakMethod(block, predictionPlayer) {
    
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
            this@PacketBreakMethod.block.broadcastDestructionStage(predictionPlayer, stage)
        } else {
            this@PacketBreakMethod.block.broadcastDestructionStage(entityId, stage)
        }
    }
    
}

internal class DisplayEntityBreakMethod(block: Block) : VisibleBreakMethod(block) {
    
    private val _breakStage = mutableProvider(-1)
    override var breakStage by _breakStage
    
    private val itemDisplay = packetItemDisplay {
        location by block.location.add(.5, .5, .5)
        metadata { itemStack by _breakStage.flatMap { items.getOrNull(it) ?: provider(ItemStack.empty()) }}
    }
    
    init {
        itemDisplay.spawn()
    }
    
    override fun stop() {
        itemDisplay.despawn()
    }
    
    companion object {
        
        private val items = (0..9).map { stage ->
            itemProvider(DefaultBlockOverlays.BREAK_STAGE_OVERLAY) {
                data[DataComponentTypes.CUSTOM_MODEL_DATA] by customModelData()
                    .addFloat(stage.toFloat())
                    .build()
            }.map(ItemProvider::get)
        }
        
    }
    
}