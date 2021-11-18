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
private var Block.fluidAmount: Long
    get() {
        val levelled = blockData
        if (levelled is Levelled) {
            return (levelled.level / levelled.maximumLevel * 1000).toLong()
        }
        
        return 0L
    }
    set(amount) {
        val levelled = blockData
        if (levelled is Levelled) {
            levelled.level = ((amount / 1000.0) * (1.0 / levelled.maximumLevel)).toInt()
        }
    }

// TODO: implement
class CauldronFluidContainer(
    uuid: UUID,
    private val cauldron: Block,
) : FluidContainer(
    uuid,
    ALLOWED_FLUID_TYPES,
    cauldron.fluidType,
    1000,
    cauldron.fluidAmount
) {
    
    override var type: FluidType?
        get() = super.type
        set(fluidType) {
            super.type = fluidType
            cauldron.fluidType = fluidType
        }
    
    override var amount: Long
        get() = super.amount
        set(amount) {
            super.amount = amount
            cauldron.fluidAmount = amount
        }
    
    fun handleBlockUpdate() {
        super.type = cauldron.fluidType
        super.amount = if (type != null) cauldron.fluidAmount else 0L
        
        callUpdateHandlers()
    }
    
}