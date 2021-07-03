package xyz.xenondevs.nova.advancement.powercell

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.RootAdvancement
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object BasicPowerCellAdvancement : Advancement(NamespacedKey(NOVA, "basic_power_cell")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.BASIC_POWER_CELL)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.BASIC_POWER_CELL.toIcon())
        }
    }
    
}