package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

object JetpackAdvancement : Advancement(NamespacedKey(NOVA, "jetpack")) {
    
    init {
        setParent(ChargerAdvancement.key)
        addObtainCriteria(NovaMaterial.JETPACK)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.JETPACK.toIcon())
        }
    }
    
}