package xyz.xenondevs.nova.player.advancement.mob

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.MOB_DUPLICATOR
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object MobDuplicatorAdvancement : Advancement(NamespacedKey(NOVA, "mob_duplicator")) {
    
    init {
        setParent(MobKillerAdvancement.key)
        addObtainCriteria(MOB_DUPLICATOR)
        setDisplayLocalized {
            it.setIcon(MOB_DUPLICATOR.toIcon())
        }
    }
    
}