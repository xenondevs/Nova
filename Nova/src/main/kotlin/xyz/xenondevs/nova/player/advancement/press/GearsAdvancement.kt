package xyz.xenondevs.nova.player.advancement.press

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object GearsAdvancement : Advancement(NamespacedKey(NOVA, "gears")) {
    
    init {
        setParent(MechanicalPressAdvancement.key)
        
        val criteria = NovaMaterialRegistry.values
            .filter { it.typeName.endsWith("GEAR") }
            .map { addObtainCriteria(it) }
        
        addRequirements(*criteria.toTypedArray())
        
        setDisplayLocalized {
            it.setIcon(NovaMaterialRegistry.COPPER_GEAR.toIcon())
        }
    }
    
}