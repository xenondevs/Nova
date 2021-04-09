package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

class EliteCableAdvancement : Advancement(KEY) {
    
    init {
        setParent(AdvancedCableAdvancement.KEY)
        addObtainCriteria(NovaMaterial.ELITE_CABLE)
        setDisplay {
            it.setTitle("Even More Energy")
            it.setDescription("Craft an Elite Cable")
            it.setIcon(NovaMaterial.ELITE_CABLE.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "elite_cable")
    }
    
}