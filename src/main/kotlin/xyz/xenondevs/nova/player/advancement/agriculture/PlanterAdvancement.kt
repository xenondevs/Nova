package xyz.xenondevs.nova.player.advancement.agriculture

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.advancement.RootAdvancement
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object PlanterAdvancement : Advancement(NamespacedKey(NOVA, "planter")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.PLANTER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.PLANTER.toIcon())
        }
    }
    
}