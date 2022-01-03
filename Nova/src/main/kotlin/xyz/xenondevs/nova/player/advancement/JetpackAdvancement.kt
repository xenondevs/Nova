package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.JETPACK

object JetpackAdvancement : Advancement(NamespacedKey(NOVA, "jetpack")) {
    
    init {
        setParent(ChargerAdvancement.key)
        addObtainCriteria(JETPACK)
        setDisplayLocalized {
            it.setIcon(JETPACK.toIcon())
        }
    }
    
}