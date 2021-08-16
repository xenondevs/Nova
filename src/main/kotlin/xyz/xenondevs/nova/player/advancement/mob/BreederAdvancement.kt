package xyz.xenondevs.nova.player.advancement.mob

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.BREEDER
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object BreederAdvancement : Advancement(NamespacedKey(NOVA, "breeder")) {
    
    init {
        setParent(BottledMobAdvancement.key)
        addObtainCriteria(BREEDER)
        setDisplayLocalized {
            it.setIcon(BREEDER.toIcon())
        }
    }
    
}