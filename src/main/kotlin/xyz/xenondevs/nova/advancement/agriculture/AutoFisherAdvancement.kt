package xyz.xenondevs.nova.advancement.agriculture

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.RootAdvancement
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.setDisplayLocalized
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object AutoFisherAdvancement : Advancement(NamespacedKey(NOVA, "auto_fisher")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(NovaMaterial.AUTO_FISHER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.AUTO_FISHER.toIcon())
        }
    }
    
}