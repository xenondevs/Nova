package xyz.xenondevs.nova.tileentity

import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.energy.EnergyStorage
import java.util.*

abstract class EnergyItemTileEntity(
    ownerUUID: UUID?,
    material: NovaMaterial,
    armorStand: ArmorStand
) : ItemTileEntity(ownerUUID, material, armorStand), EnergyStorage {
    
    protected abstract val defaultEnergyConfig: MutableMap<BlockFace, EnergyConnectionType>
    override val energyConfig: MutableMap<BlockFace, EnergyConnectionType> by lazy { retrieveData("energyConfig") { defaultEnergyConfig } }
    
    protected var energy: Int = retrieveData("energy") { 0 }
        set(value) {
            field = value
            hasEnergyChanged = true
        }
    protected var hasEnergyChanged = true
    override val providedEnergy: Int
        get() = energy
    override val requestedEnergy = 0
    
    override fun addEnergy(energy: Int) {
        this.energy += energy
    }
    
    override fun removeEnergy(energy: Int) {
        this.energy -= energy
    }
    
    override fun saveData() {
        super.saveData()
        storeData("energy", energy, true)
        storeData("energyConfig", energyConfig)
    }
    
}