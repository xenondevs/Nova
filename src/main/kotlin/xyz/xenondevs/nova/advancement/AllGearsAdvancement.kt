package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

class AllGearsAdvancement : Advancement(KEY) {
    
    init {
        setParent(GearsAdvancement.KEY)
    
        NovaMaterial.values()
            .filter { it.name.endsWith("GEAR") }
            .forEach { addObtainCriteria(it) }
    
        setDisplay {
            it.setTitle("All the Gears")
            it.setDescription("Get one of every gear")
            it.setIcon(NovaMaterial.DIAMOND_GEAR.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "all_gears")
    }
    
}