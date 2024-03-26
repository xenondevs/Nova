package xyz.xenondevs.nova.ui.waila.info.line

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

private const val ENERGY_BAR_LENGTH = 40

object EnergyHolderLine {
    
    // TODO
    
//    fun getEnergyBarLine(holder: NovaEnergyHolder): WailaLine {
//        return WailaLine(
//            createEnergyBarString(holder.energy, holder.maxEnergy),
//            WailaLine.Alignment.CENTERED
//        )
//    }
//    
//    fun getEnergyAmountLine(holder: NovaEnergyHolder): WailaLine {
//        return WailaLine(
//            Component.text(NumberFormatUtils.getEnergyString(holder.energy, holder.maxEnergy), NamedTextColor.GRAY),
//            WailaLine.Alignment.CENTERED
//        )
//    }
//    
//    fun getEnergyDeltaLine(holder: NovaEnergyHolder): WailaLine {
//        return WailaLine(
//            Component.text()
//                .append(Component.text("+", NamedTextColor.GRAY))
//                .append(Component.translatable("menu.nova.energy_per_tick", Component.text(NumberFormatUtils.getEnergyString(holder.energyPlus))))
//                .append(Component.text(" | -"))
//                .append(Component.translatable("menu.nova.energy_per_tick", Component.text(NumberFormatUtils.getEnergyString(holder.energyMinus))))
//                .build(),
//            WailaLine.Alignment.CENTERED
//        )
//    }
    
    private fun createEnergyBarString(energy: Long, maxEnergy: Long): Component {
        val percentage = energy.toDouble() / maxEnergy.toDouble()
        
        val green = (ENERGY_BAR_LENGTH * percentage).toInt()
        val red = ENERGY_BAR_LENGTH - green
        
        return Component.text()
            .append(Component.text("[", NamedTextColor.DARK_GRAY))
            .append(Component.text("|".repeat(green), NamedTextColor.GREEN))
            .append(Component.text("|".repeat(red), NamedTextColor.RED))
            .append(Component.text("]", NamedTextColor.DARK_GRAY))
            .build()
    }
    
}