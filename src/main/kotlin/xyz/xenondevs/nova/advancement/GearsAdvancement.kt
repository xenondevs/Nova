package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

class GearsAdvancement : Advancement(KEY) {
    
    init {
        setParent(MechanicalPressAdvancement.KEY)
    
        val criteria = NovaMaterial.values()
            .filter { it.name.endsWith("GEAR") }
            .map { addObtainCriteria(it) }
    
        addRequirements(*criteria.toTypedArray())
        
        setDisplay {
            it.setTitle("Gears")
            it.setDescription("Make a gear using a Mechanical Press")
            it.setIcon(NovaMaterial.IRON_GEAR.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "gears")
    }
    
}