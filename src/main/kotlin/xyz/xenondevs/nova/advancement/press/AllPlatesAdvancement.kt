package xyz.xenondevs.nova.advancement.press

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object AllPlatesAdvancement : Advancement(NamespacedKey(NOVA, "all_plates")) {
    
    init {
        setParent(PlatesAdvancement.key)
        
        NovaMaterial.values()
            .filter { it.name.endsWith("PLATE") }
            .forEach { addObtainCriteria(it) }
        
        setDisplayLocalized {
            it.setIcon(NovaMaterial.DIAMOND_PLATE.toIcon())
        }
    }
    
}