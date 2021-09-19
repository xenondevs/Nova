package xyz.xenondevs.nova.player.advancement.press

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object AllPlatesAdvancement : Advancement(NamespacedKey(NOVA, "all_plates")) {
    
    init {
        setParent(PlatesAdvancement.key)
        
        NovaMaterialRegistry.values
            .filter { it.typeName.endsWith("PLATE") }
            .forEach { addObtainCriteria(it) }
        
        setDisplayLocalized {
            it.setIcon(NovaMaterialRegistry.DIAMOND_PLATE.toIcon())
        }
    }
    
}