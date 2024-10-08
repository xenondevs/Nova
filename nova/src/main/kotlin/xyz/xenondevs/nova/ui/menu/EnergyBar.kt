package xyz.xenondevs.nova.ui.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.item.builder.addLoreLines
import xyz.xenondevs.invui.item.impl.AutoUpdateItem
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.EnergyNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.DefaultEnergyHolder
import xyz.xenondevs.nova.world.item.DefaultGuiItems

/**
 * A multi-item gui component for displaying energy levels.
 */
class EnergyBar(
    height: Int,
    energy: Provider<Long>,
    maxEnergy: Provider<Long>,
    energyPlus: Provider<Long>,
    energyMinus: Provider<Long>,
) : VerticalBar(height) {
    
    private val energy by energy
    private val maxEnergy by maxEnergy
    private val energyPlus by energyPlus
    private val energyMinus by energyMinus
    
    constructor(height: Int, energyHolder: DefaultEnergyHolder) : this(
        height,
        energyHolder.energyProvider, energyHolder.maxEnergyProvider,
        energyHolder.energyPlusProvider, energyHolder.energyMinusProvider
    )
    
    override fun createBarItem(section: Int) = AutoUpdateItem(1) {
        val builder = createItemBuilder(DefaultGuiItems.BAR_RED, section, energy.toDouble() / maxEnergy.toDouble())
        
        if (energy == Long.MAX_VALUE) {
            builder.setDisplayName("∞ J / ∞ J")
        } else {
            builder.setDisplayName(NumberFormatUtils.getEnergyString(energy, maxEnergy))
        }
        
        if (energyPlus > 0) {
            builder.addLoreLines(Component.translatable(
                "menu.nova.energy_per_tick",
                NamedTextColor.GRAY,
                Component.text("+" + NumberFormatUtils.getEnergyString(energyPlus / EnergyNetwork.TICK_DELAY_PROVIDER.get()))
            ))
        }
        
        if (energyMinus > 0) {
            builder.addLoreLines(Component.translatable(
                "menu.nova.energy_per_tick",
                NamedTextColor.GRAY,
                Component.text("-" + NumberFormatUtils.getEnergyString(energyMinus / EnergyNetwork.TICK_DELAY_PROVIDER.get()))
            ))
        }
        
        builder
    }
    
}
