package xyz.xenondevs.nova.player.advancement.powercell

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.advancement.RootAdvancement
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object BasicPowerCellAdvancement : Advancement(NamespacedKey(NOVA, "basic_power_cell")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.BASIC_POWER_CELL)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.BASIC_POWER_CELL.toIcon())
        }
    }
    
}