package xyz.xenondevs.nova.world.block.logic.`break`

import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.material.CoreBlockOverlay
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.sendDestructionPacket
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.fakeentity.impl.FakeArmorStand
import xyz.xenondevs.nova.world.pos
import kotlin.random.Random

interface BreakMethod {
    
    val hasClientsidePrediction: Boolean
    var breakStage: Int
    fun stop()
    
    companion object {
        
        private val INVISIBLE = object: BreakMethod {
            override val hasClientsidePrediction = false
            override var breakStage: Int = 0
            override fun stop() {}
        }
        
        fun of(block: Block, material: BlockNovaMaterial, entityId: Int = Random.nextInt()): BreakMethod? =
            of(block, material, null, entityId)
        
        fun of(
            block: Block,
            material: BlockNovaMaterial,
            predictionPlayer: Player?,
            entityId: Int = predictionPlayer?.entityId ?: Random.nextInt()
        ): BreakMethod {
            return if (material.showBreakAnimation)
                if (block.type == Material.BARRIER) ArmorStandBreakMethod(block.pos)
                else PacketBreakMethod(block.pos, entityId, predictionPlayer)
            else INVISIBLE
        }
        
    }
    
}

internal abstract class VisibleBreakMethod(val pos: BlockPos, val predictionPlayer: Player? = null): BreakMethod {
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
            block.sendDestructionPacket(predictionPlayer, stage)
        } else {
            block.sendDestructionPacket(entityId, stage)
        }
    }
    
}

internal class ArmorStandBreakMethod(pos: BlockPos) : VisibleBreakMethod(pos) {
    
    private val armorStand = FakeArmorStand(pos.location.center(), true) { _, data ->
        data.isInvisible = true
        data.isMarker = true
    }
    
    override var breakStage: Int = -1
        set(stage) {
            if (field == stage) return
            
            field = stage
            if (stage in 0..9) {
                armorStand.setEquipment(EquipmentSlot.HEAD, CoreBlockOverlay.BREAK_STAGE_OVERLAY.clientsideProviders[stage].get(), true)
            } else {
                armorStand.setEquipment(EquipmentSlot.HEAD, null, true)
            }
        }
    
    override fun stop() {
        armorStand.remove()
    }
    
}