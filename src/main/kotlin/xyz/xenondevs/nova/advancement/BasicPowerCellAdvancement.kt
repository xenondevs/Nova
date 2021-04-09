package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

class BasicPowerCellAdvancement : Advancement(KEY) {
    
    init {
        setParent(RootAdvancement.KEY)
        addObtainCriteria(NovaMaterial.POWER_CELL)
        setDisplay {
            it.setTitle("Storing Energy")
            it.setDescription("Craft a Power Cell")
            it.setIcon(NovaMaterial.POWER_CELL.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "basic_power_cell")
    }
    
}