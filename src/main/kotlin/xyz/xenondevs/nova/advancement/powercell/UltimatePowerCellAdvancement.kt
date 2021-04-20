package xyz.xenondevs.nova.advancement.powercell

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

class UltimatePowerCellAdvancement : Advancement(KEY) {
    
    init {
        setParent(ElitePowerCellAdvancement.KEY)
        addObtainCriteria(NovaMaterial.ULTIMATE_POWER_CELL)
        setDisplay {
            it.setTitle("Is this the Future?")
            it.setDescription("Craft an Ultimate Power Cell")
            it.setIcon(NovaMaterial.ULTIMATE_POWER_CELL.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "ultimate_power_cell")
    }
    
}