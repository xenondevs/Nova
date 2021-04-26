package xyz.xenondevs.nova.advancement.press

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object AllGearsAdvancement : Advancement(NamespacedKey(NOVA, "all_gears")) {
    
    init {
        setParent(GearsAdvancement.key)
        
        NovaMaterial.values()
            .filter { it.name.endsWith("GEAR") }
            .forEach { addObtainCriteria(it) }
        
        setDisplay {
            it.setTitle("All the Gears")
            it.setDescription("Get one of every gear")
            it.setIcon(NovaMaterial.DIAMOND_GEAR.toIcon())
        }
    }
    
}