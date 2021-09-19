package xyz.xenondevs.nova.player.advancement.press

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object AllGearsAdvancement : Advancement(NamespacedKey(NOVA, "all_gears")) {
    
    init {
        setParent(GearsAdvancement.key)
        
        NovaMaterialRegistry.values
            .filter { it.typeName.endsWith("GEAR") }
            .forEach { addObtainCriteria(it) }
        
        setDisplayLocalized {
            it.setIcon(NovaMaterialRegistry.DIAMOND_GEAR.toIcon())
        }
    }
    
}