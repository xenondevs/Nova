package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.item.ItemBuilder
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer

class FluidBar(
    gui: GUI,
    x: Int, y: Int,
    height: Int,
    private val fluidContainer: FluidContainer
) : VerticalBar(gui, x, y, height) {
    
    override val barMaterial: NovaMaterial
        get() = when (fluidContainer.type) {
            FluidType.WATER -> NovaMaterialRegistry.BLUE_BAR
            else -> NovaMaterialRegistry.ORANGE_BAR
        }
    
    private var amount = 0L
    private var capacity = 0L
    
    init {
        fluidContainer.updateHandlers += ::update
        update()
    }
    
    fun update() {
        amount = fluidContainer.amount
        capacity = fluidContainer.capacity
        percentage = (amount.toDouble() / capacity.toDouble()).coerceIn(0.0, 1.0)
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        if (amount == Long.MAX_VALUE) {
            itemBuilder.setDisplayName("∞ mB / ∞ mB")
        } else {
            val capacity = if (capacity == Long.MAX_VALUE) "∞" else capacity.toString()
            itemBuilder.setDisplayName("$amount mB / $capacity mB")
        }
        return itemBuilder
    }
    
}