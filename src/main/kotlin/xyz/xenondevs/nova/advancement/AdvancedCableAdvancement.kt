package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

class AdvancedCableAdvancement : Advancement(KEY) {
    
    init {
        setParent(BasicCableAdvancement.KEY)
        addObtainCriteria(NovaMaterial.ADVANCED_CABLE)
        setDisplay {
            it.setTitle("Advanced Cable Technology")
            it.setDescription("Craft an Advanced Cable")
            it.setIcon(NovaMaterial.ADVANCED_CABLE.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "advanced_cable")
    }
    
}