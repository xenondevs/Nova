package xyz.xenondevs.nova.tileentity.network.fluid.container

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Levelled
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import java.util.*

private val ALLOWED_FLUID_TYPES = hashSetOf(FluidType.WATER, FluidType.LAVA)

private var Block.fluidType: FluidType?
    get() = when (type) {
        Material.LAVA_CAULDRON -> FluidType.LAVA
        Material.WATER_CAULDRON -> FluidType.WATER
        Material.CAULDRON -> FluidType.NONE
        else -> null
    }
    set(fluidType) {
        type = when (fluidType) {
            FluidType.NONE -> Material.CAULDRON
            FluidType.LAVA -> Material.LAVA_CAULDRON
            FluidType.WATER -> Material.WATER_CAULDRON
            null -> throw IllegalArgumentException()
        }
    }

// This will allow players to cheat small amounts of fluids, but that shouldn't be a big issue.
private fun Block.setFluidAmount(amount: Long, type: FluidType?) {
    require(amount <= 1000)
    
    when (type) {
        FluidType.WATER -> {
            val level = (amount / 333).toInt()
            if (level > 0) {
                var levelled = blockData as? Levelled
                if (levelled == null) {
                    fluidType = type
                    levelled = blockData as Levelled
                }
                
                levelled.level = level
                blockData = levelled
            } else this.type = Material.CAULDRON
        }
        
        FluidType.LAVA -> {
            this.type = if (amount == 1000L)
                Material.LAVA_CAULDRON
            else Material.CAULDRON
        }
        
        else -> this.type = Material.CAULDRON
    }
}

private val Block.fluidAmount: Long
    get() {
        if (type == Material.WATER_CAULDRON) {
            val levelled = blockData as Levelled
            return levelled.level * 333L + 1
        } else if (type == Material.LAVA_CAULDRON) {
            return 1000L
        }
        
        return 0L
    }

internal class CauldronFluidContainer(
    uuid: UUID,
    private val cauldron: Block,
) : FluidContainer(
    uuid,
    ALLOWED_FLUID_TYPES,
    cauldron.fluidType,
    cauldron.fluidAmount,
    1000
) {
    
    private var selfUpdate = false
    
    override var amount: Long
        get() = super.amount
        set(amount) {
            if (super.amount == amount)
                return
            
            super.amount = amount
            
            selfUpdate = true
            cauldron.setFluidAmount(amount, type)
            selfUpdate = false
        }
    
    fun handleBlockUpdate() {
        if (selfUpdate)
            return
        
        super.type = cauldron.fluidType
        super.amount = cauldron.fluidAmount
        
        callUpdateHandlers()
    }
    
}