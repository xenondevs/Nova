package xyz.xenondevs.nova.advancement.powercell

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object AdvancedPowerCellAdvancement : Advancement(NamespacedKey(NOVA, "advanced_power_cell")) {
    
    init {
        setParent(BasicPowerCellAdvancement.key)
        addObtainCriteria(NovaMaterial.ADVANCED_POWER_CELL)
        setDisplay {
            it.setTitle("Even More Energy")
            it.setDescription("Craft an Advanced Power Cell")
            it.setIcon(NovaMaterial.ADVANCED_POWER_CELL.toIcon())
        }
    }
    
}