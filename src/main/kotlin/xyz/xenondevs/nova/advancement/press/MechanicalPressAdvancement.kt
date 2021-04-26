package xyz.xenondevs.nova.advancement.press

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.RootAdvancement
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object MechanicalPressAdvancement : Advancement(NamespacedKey(NOVA, "mechanical_press")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.MECHANICAL_PRESS)
        setDisplay {
            it.setTitle("Pressing Metal")
            it.setDescription("Craft a Mechanical Press")
            it.setIcon(NovaMaterial.MECHANICAL_PRESS.toIcon())
        }
    }
    
}