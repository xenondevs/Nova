package xyz.xenondevs.nova.advancement.cable

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

class UltimateCableAdvancement : Advancement(KEY) {
    
    init {
        setParent(EliteCableAdvancement.KEY)
        addObtainCriteria(NovaMaterial.ULTIMATE_CABLE)
        setDisplay {
            it.setTitle("The Ultimate Cable")
            it.setDescription("Craft an Ultimate Cable")
            it.setIcon(NovaMaterial.ULTIMATE_CABLE.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "ultimate_cable")
    }
    
}