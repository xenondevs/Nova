package xyz.xenondevs.nova.world.block.logic.`break`

import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Material
import org.bukkit.block.Block
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.material.CoreBlockOverlay
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.sendDestructionPacket
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.fakeentity.impl.FakeArmorStand
import xyz.xenondevs.nova.world.pos
import kotlin.random.Random

internal abstract class BreakMethod(val pos: BlockPos) {
    
    val block = pos.block
    
    abstract var breakStage: Int
    
    abstract fun stop()
    
    companion object {
        
        fun of(block: Block, material: BlockNovaMaterial, entityId: Int = Random.nextInt()): BreakMethod? {
            return if (material.showBreakAnimation)
                if (block.type == Material.BARRIER) ArmorStandBreakMethod(block.pos)
                else PacketBreakMethod(block.pos, entityId)
            else null
        }
        
    }
    
}

internal class PacketBreakMethod(pos: BlockPos, private val entityId: Int) : BreakMethod(pos) {
    
    override var breakStage: Int = -1
        set(stage) {
            if (field == stage) return
            
            field = stage
            block.sendDestructionPacket(entityId, stage)
        }
    
    override fun stop() {
        block.sendDestructionPacket(entityId, -1)
    }
    
}

internal class ArmorStandBreakMethod(pos: BlockPos) : BreakMethod(pos) {
    
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