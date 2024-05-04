package xyz.xenondevs.nova.ui.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.addLoreLines
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.tileentity.network.type.energy.holder.DefaultEnergyHolder
import xyz.xenondevs.nova.util.NumberFormatUtils

class EnergyBar(
    height: Int,
    private val getEnergy: () -> Long,
    private val getMaxEnergy: () -> Long,
    private val getEnergyPlus: () -> Long,
    private val getEnergyMinus: () -> Long,
) : VerticalBar(height) {

    override val barItem = DefaultGuiItems.BAR_RED

    private var energy: Long = 0
    private var maxEnergy: Long = 0

    constructor(height: Int, energyHolder: DefaultEnergyHolder) : this(
        height,
        energyHolder::energy, energyHolder::maxEnergy,
        energyHolder::energyPlus, energyHolder::energyMinus
    ) {
        energyHolder.updateHandlers += ::update
    }
    
    init {
        update()
    }

    fun update() {
        energy = getEnergy()
        maxEnergy = getMaxEnergy()
        percentage = (energy.toDouble() / maxEnergy.toDouble()).coerceIn(0.0, 1.0)
    }

    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        if (energy == Long.MAX_VALUE) itemBuilder.setDisplayName("∞ J / ∞ J")
        else itemBuilder.setDisplayName(NumberFormatUtils.getEnergyString(energy, maxEnergy))

        val energyPlus = getEnergyPlus()
        val energyMinus = getEnergyMinus()
        
        if (energyPlus > 0) {
            itemBuilder.addLoreLines(Component.translatable(
                "menu.nova.energy_per_tick",
                NamedTextColor.GRAY,
                Component.text("+" + NumberFormatUtils.getEnergyString(energyPlus))
            ))
        }
        if (energyMinus > 0) {
            itemBuilder.addLoreLines(Component.translatable(
                "menu.nova.energy_per_tick",
                NamedTextColor.GRAY,
                Component.text("-" + NumberFormatUtils.getEnergyString(energyMinus))
            ))
        }
        return itemBuilder
    }

}
