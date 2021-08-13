package xyz.xenondevs.nova.player.advancement.press

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.advancement.RootAdvancement
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object MechanicalPressAdvancement : Advancement(NamespacedKey(NOVA, "mechanical_press")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.MECHANICAL_PRESS)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.MECHANICAL_PRESS.toIcon())
        }
    }
    
}