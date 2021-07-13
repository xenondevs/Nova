package xyz.xenondevs.nova.advancement.agriculture

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object FertilizerAdvancement : Advancement(NamespacedKey(NOVA, "fertilizer")) {
    
    init {
        setParent(PlanterAdvancement.key)
        addObtainCriteria(NovaMaterial.FERTILIZER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.FERTILIZER.toIcon())
        }
    }
    
}