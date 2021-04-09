package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

class MechanicalPressAdvancement : Advancement(KEY) {
    
    init {
        setParent(RootAdvancement.KEY)
        addObtainCriteria(NovaMaterial.MECHANICAL_PRESS)
        setDisplay {
            it.setTitle("Pressing Metal")
            it.setDescription("Craft a Mechanical Press")
            it.setIcon(NovaMaterial.MECHANICAL_PRESS.toIcon())
        }
    }
    
    companion object {
        val KEY = NamespacedKey(NOVA, "mechanical_press")
    }
    
}