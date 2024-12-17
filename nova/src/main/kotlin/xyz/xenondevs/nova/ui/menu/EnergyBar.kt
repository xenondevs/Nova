package xyz.xenondevs.nova.ui.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.EnergyNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.DefaultEnergyHolder
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * A multi-item gui component for displaying energy levels.
 */
class EnergyBar @JvmOverloads constructor( // TODO: Remove @JvmOverloads in 0.19
    height: Int,
    private val energy: Provider<Long>,
    private val maxEnergy: Provider<Long>,
    private val getEnergyPlus: () -> Long,
    private val getEnergyMinus: () -> Long,
    private val item: NovaItem = DefaultGuiItems.BAR_RED
) : VerticalBar(height) {
    
    @JvmOverloads
    constructor(height: Int, energyHolder: DefaultEnergyHolder, item: NovaItem = DefaultGuiItems.BAR_RED) : this(
        height,
        energyHolder.energyProvider, energyHolder.maxEnergyProvider,
        { energyHolder.energyPlus }, { energyHolder.energyMinus },
        item
    )
    
    override fun createBarItem(section: Int) =
        Item.builder()
            .updatePeriodically(1)
            .setItemProvider {
                val energy = energy.get()
                val maxEnergy = maxEnergy.get()
                val energyPlus = getEnergyPlus()
                val energyMinus = getEnergyMinus()
                
                val builder = createItemBuilder(item, section, energy.toDouble() / maxEnergy.toDouble())
                
                if (energy == Long.MAX_VALUE) {
                    builder.setName("∞ J / ∞ J")
                } else {
                    builder.setName(NumberFormatUtils.getEnergyString(energy, maxEnergy))
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
            }.build()
    
}
