package xyz.xenondevs.nova.advancement.press

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object PlatesAdvancement : Advancement(NamespacedKey(NOVA, "plates")) {
    
    init {
        setParent(MechanicalPressAdvancement.key)
        
        val criteria = NovaMaterial.values()
            .filter { it.name.endsWith("PLATE") }
            .map { addObtainCriteria(it) }
        
        addRequirements(*criteria.toTypedArray())
        
        setDisplayLocalized {
            it.setIcon(NovaMaterial.COPPER_PLATE.toIcon())
        }
    }
    
}