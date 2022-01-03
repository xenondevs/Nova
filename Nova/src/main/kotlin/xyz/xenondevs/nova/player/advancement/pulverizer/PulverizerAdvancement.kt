package xyz.xenondevs.nova.player.advancement.pulverizer

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.PULVERIZER
import xyz.xenondevs.nova.player.advancement.RootAdvancement
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object PulverizerAdvancement : Advancement(NamespacedKey(NOVA, "pulverizer")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(PULVERIZER)
        setDisplayLocalized {
            it.setIcon(PULVERIZER.toIcon())
        }
    }
    
}