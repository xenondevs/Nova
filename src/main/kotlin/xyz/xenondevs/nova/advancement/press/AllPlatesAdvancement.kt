package xyz.xenondevs.nova.advancement.press

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

class AllPlatesAdvancement : Advancement(KEY) {
    
    init {
        setParent(PlatesAdvancement.KEY)
    
        NovaMaterial.values()
            .filter { it.name.endsWith("PLATE") }
            .forEach { addObtainCriteria(it) }
    
        setDisplay {
            it.setTitle("All the Plates")
            it.setDescription("Get one of every plate")
            it.setIcon(NovaMaterial.DIAMOND_PLATE.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "all_plates")
    }
    
}