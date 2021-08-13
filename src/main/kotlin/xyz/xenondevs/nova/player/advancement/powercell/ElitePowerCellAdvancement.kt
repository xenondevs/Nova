package xyz.xenondevs.nova.player.advancement.powercell

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object ElitePowerCellAdvancement : Advancement(NamespacedKey(NOVA, "elite_power_cell")) {
    
    init {
        setParent(AdvancedPowerCellAdvancement.key)
        addObtainCriteria(NovaMaterial.ELITE_POWER_CELL)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.ELITE_POWER_CELL.toIcon())
        }
    }
    
}