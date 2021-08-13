package xyz.xenondevs.nova.player.advancement.mob

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object MobKillerAdvancement : Advancement(NamespacedKey(NOVA, "mob_killer")) {
    
    init {
        setParent(BreederAdvancement.key)
        addObtainCriteria(NovaMaterial.MOB_KILLER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.MOB_KILLER.toIcon())
        }
    }
    
}