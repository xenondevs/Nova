package xyz.xenondevs.nova.ui.waila.info.line

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import xyz.xenondevs.nova.tileentity.network.energy.holder.NovaEnergyHolder
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.data.MovingComponentBuilder

private const val ENERGY_BAR_LENGTH = 40

object EnergyHolderLine {
    
    fun getEnergyBarLine(player: Player, holder: NovaEnergyHolder): WailaLine {
        return WailaLine(
            MovingComponentBuilder(player.locale)
                .append(createEnergyBarString(holder.energy, holder.maxEnergy)),
            WailaLine.Alignment.CENTERED
        )
    }
    
    fun getEnergyAmountLine(player: Player, holder: NovaEnergyHolder): WailaLine {
        return WailaLine(
            MovingComponentBuilder(player.locale)
                .append(NumberFormatUtils.getEnergyString(holder.energy, holder.maxEnergy))
                .color(ChatColor.GRAY),
            WailaLine.Alignment.CENTERED
        )
    }
    
    fun getEnergyDeltaLine(player: Player, holder: NovaEnergyHolder): WailaLine {
        return WailaLine(
            MovingComponentBuilder(player.locale)
                .color(ChatColor.GRAY)
                .append("+")
                .append(TranslatableComponent("menu.nova.energy_per_tick", NumberFormatUtils.getEnergyString(holder.energyPlus)))
                .append(" | -")
                .append(TranslatableComponent("menu.nova.energy_per_tick", NumberFormatUtils.getEnergyString(holder.energyMinus))),
            WailaLine.Alignment.CENTERED
        )
    }
    
    private fun createEnergyBarString(energy: Long, maxEnergy: Long): Array<BaseComponent> {
        val percentage = energy.toDouble() / maxEnergy.toDouble()
        
        val green = (ENERGY_BAR_LENGTH * percentage).toInt()
        val red = ENERGY_BAR_LENGTH - green
        
        return ComponentBuilder()
            .append("[")
            .color(ChatColor.DARK_GRAY)
            .append("|".repeat(green))
            .color(ChatColor.GREEN)
            .append("|".repeat(red))
            .color(ChatColor.RED)
            .append("]")
            .color(ChatColor.DARK_GRAY)
            .create()
    }
    
}