package xyz.xenondevs.nova.world.block.model

import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Material
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.tileentity.requiresLight
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.yaw
import xyz.xenondevs.nova.world.armorstand.ArmorStandDataHolder
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
            ) { ast, data -> setArmorStandValues(ast, data, i + 1) }
        }
    }
    
    private fun setArmorStandValues(armorStand: FakeArmorStand, data: ArmorStandDataHolder, subId: Int = 0) {
        data.invisible = true
        data.marker = true
        data.onFire = material.hitboxType.requiresLight
        armorStand.setEquipment(EquipmentSlot.HEAD, material.blockProviders[subId].get(), false)
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
        armorStands[0].setEquipment(EquipmentSlot.HEAD, material.blockProviders[subId].get(), true)
    }
    
    companion object : BlockModelProviderType<ArmorStandModelProvider> {
        override fun create(blockState: NovaBlockState) = ArmorStandModelProvider(blockState)
    }
    
}