package xyz.xenondevs.nova.world.block.model

import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Material
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.tileentity.requiresLight
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.yaw
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand

class ArmorStandModelProvider(blockState: NovaBlockState) : BlockModelProvider {
    
    private val pos = blockState.pos
    private val material = blockState.material
    
    private val armorStands = ArrayList<FakeArmorStand>()
    private val multiBlockPositions = material.multiBlockLoader?.invoke(pos)
    
    init {
        val location = pos.location.center()
        
        val directional = blockState.getProperty(Directional)
        location.yaw = directional?.facing?.yaw ?: 180f // by default, look north (180°)
        
        armorStands += FakeArmorStand(location, false, ::setArmorStandValues)
        
        multiBlockPositions?.forEachIndexed { i, otherPos ->
            armorStands += FakeArmorStand(
                otherPos.location.center().apply { yaw = location.yaw }, false
            ) { setArmorStandValues(it, i + 1) }
        }
    }
    
    private fun setArmorStandValues(armorStand: FakeArmorStand, subId: Int = 0) {
        armorStand.isInvisible = true
        armorStand.isMarker = true
        armorStand.setSharedFlagOnFire(material.hitboxType.requiresLight)
        armorStand.setEquipment(EquipmentSlot.HEAD, material.block.createClientsideItemStack(subId))
    }
    
    override fun load(placed: Boolean) {
        if (placed) {
            pos.block.type = material.hitboxType
            multiBlockPositions?.forEach { it.block.type = material.hitboxType }
        }
        armorStands.forEach(FakeArmorStand::register)
    }
    
    override fun remove(broken: Boolean) {
        armorStands.forEach(FakeArmorStand::remove)
        if (broken) {
            pos.block.type = Material.AIR
            multiBlockPositions?.forEach { it.block.type = Material.AIR }
        }
    }
    
    override fun update(subId: Int) {
        armorStands[0].setEquipment(EquipmentSlot.HEAD, material.block.createClientsideItemStack(subId))
        armorStands[0].updateEquipment()
    }
    
    companion object : BlockModelProviderType<ArmorStandModelProvider> {
        override fun create(blockState: NovaBlockState) = ArmorStandModelProvider(blockState)
    }
    
}