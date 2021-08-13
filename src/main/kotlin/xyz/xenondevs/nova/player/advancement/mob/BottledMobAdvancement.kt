package xyz.xenondevs.nova.player.advancement.mob

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.advancement.RootAdvancement
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object BottledMobAdvancement : Advancement(NamespacedKey(NOVA, "bottled_mob")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.BOTTLED_MOB)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.BOTTLED_MOB.toIcon())
        }
    }
    
}