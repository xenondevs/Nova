package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

class PlatesAdvancement : Advancement(KEY) {
    
    init {
        setParent(MechanicalPressAdvancement.KEY)
        
        val criteria = NovaMaterial.values()
            .filter { it.name.endsWith("PLATE") }
            .map { addObtainCriteria(it) }
        
        addRequirements(*criteria.toTypedArray())
        
        setDisplay {
            it.setTitle("Plates")
            it.setDescription("Make a plate using a Mechanical Press")
            it.setIcon(NovaMaterial.IRON_PLATE.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "plates")
    }
    
}