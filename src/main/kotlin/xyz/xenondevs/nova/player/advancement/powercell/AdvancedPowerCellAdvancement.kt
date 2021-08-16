package xyz.xenondevs.nova.player.advancement.powercell

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ADVANCED_POWER_CELL
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object AdvancedPowerCellAdvancement : Advancement(NamespacedKey(NOVA, "advanced_power_cell")) {
    
    init {
        setParent(BasicPowerCellAdvancement.key)
        addObtainCriteria(ADVANCED_POWER_CELL)
        setDisplayLocalized {
            it.setIcon(ADVANCED_POWER_CELL.toIcon())
        }
    }
    
}