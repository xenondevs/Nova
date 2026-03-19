package xyz.xenondevs.nova.ui.menu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.invui.dsl.ClickDsl
import xyz.xenondevs.invui.dsl.property.by
import xyz.xenondevs.invui.gui.SlotElementSupplier
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.EnergyNetwork
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.DefaultEnergyHolder
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * A [verticalBar] for displaying an energy level.
 * Ranges from [DefaultEnergyHolder.energyProvider] to [DefaultEnergyHolder.maxEnergyProvider].
 * Shows [DefaultEnergyHolder.energyPlusProvider] and [DefaultEnergyHolder.energyMinusProvider] in the lore.
 * Uses [barType] as the bar item type.
 */
fun energyBar(
    energyHolder: DefaultEnergyHolder,
    barType: Provider<NovaItem> = DefaultGuiItems.TP_BAR_RED,
    onClick: ClickDsl.() -> Unit = {}
): SlotElementSupplier = energyBar(
    energyHolder.energyProvider,
    energyHolder.maxEnergyProvider,
    energyHolder.energyPlusProvider,
    energyHolder.energyMinusProvider,
    barType,
    onClick
)

/**
 * A [verticalBar] for displaying an energy level.
 * Ranges from [energy] to [maxEnergy].
 * Shows [energyPlus] and [energyMinus] in the lore.
 * Uses [barType] as the bar item type.
 */
fun energyBar(
    energy: Provider<Long>,
    maxEnergy: Provider<Long>,
    energyPlus: Provider<Long>,
    energyMinus: Provider<Long>,
    barType: Provider<NovaItem> = DefaultGuiItems.TP_BAR_RED,
    onClick: ClickDsl.() -> Unit = {}
): SlotElementSupplier = verticalBar(
    percentage = combinedProvider(energy, maxEnergy) { energy, maxEnergy -> energy.toDouble() / maxEnergy.toDouble() },
    barType = barType,
    modifyItemProvider = {
        name by combinedProvider(energy, maxEnergy) { energy, maxEnergy ->
            when {
                energy == Long.MAX_VALUE -> "∞ J / ∞ J"
                else -> NumberFormatUtils.getEnergyString(energy, maxEnergy)
            }
        }
        lore by combinedProvider(energyPlus, energyMinus, EnergyNetwork.TICK_DELAY_PROVIDER) { energyPlus, energyMinus, tickDelay ->
            buildList {
                if (energyPlus > 0) {
                    this += Component.translatable(
                        "menu.nova.energy_per_tick",
                        NamedTextColor.GRAY,
                        Component.text("+" + NumberFormatUtils.getEnergyString(energyPlus / tickDelay))
                    )
                }
                
                if (energyMinus > 0) {
                    this += Component.translatable(
                        "menu.nova.energy_per_tick",
                        NamedTextColor.GRAY,
                        Component.text("-" + NumberFormatUtils.getEnergyString(energyMinus / tickDelay))
                    )
                }
            }
        }
    },
    onClick = onClick
)

/**
 * A multi-item gui component for displaying energy levels.
 */
class EnergyBar(
    height: Int,
    private val energy: Provider<Long>,
    private val maxEnergy: Provider<Long>,
    private val getEnergyPlus: () -> Long,
    private val getEnergyMinus: () -> Long,
    private val item: Provider<NovaItem> = DefaultGuiItems.BAR_RED
) : VerticalBar(height) {
    
    constructor(height: Int, energyHolder: DefaultEnergyHolder, item: Provider<NovaItem> = DefaultGuiItems.BAR_RED) : this(
        height,
        energyHolder.energyProvider, energyHolder.maxEnergyProvider,
        { energyHolder.energyPlus }, { energyHolder.energyMinus },
        item
    )
    
    override fun createBarItem(section: Int): Item =
        Item.builder()
            .updatePeriodically(1)
            .setItemProvider {
                val energy = energy.get()
                val maxEnergy = maxEnergy.get()
                val energyPlus = getEnergyPlus()
                val energyMinus = getEnergyMinus()
                
                val builder = createItemBuilder(item.get(), section, energy.toDouble() / maxEnergy.toDouble())
                
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
