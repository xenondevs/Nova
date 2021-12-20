package xyz.xenondevs.nova.player.advancement.power

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.LIGHTNING_EXCHANGER
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object LightningExchangerAdvancement : Advancement(NamespacedKey(NOVA, "lightning_exchanger")) {
    
    init {
        setParent(WindTurbineAdvancement.key)
        addObtainCriteria(LIGHTNING_EXCHANGER)
        setDisplayLocalized {
            it.setIcon(LIGHTNING_EXCHANGER.toIcon())
        }
    }
    
}