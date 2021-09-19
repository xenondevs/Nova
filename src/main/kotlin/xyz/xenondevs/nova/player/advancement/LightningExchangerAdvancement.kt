package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.LIGHTNING_EXCHANGER

object LightningExchangerAdvancement : Advancement(NamespacedKey(NOVA, "lightning_exchanger")) {
    
    init {
        setParent(SolarPanelAdvancement.key)
        addObtainCriteria(LIGHTNING_EXCHANGER)
        setDisplayLocalized {
            it.setIcon(LIGHTNING_EXCHANGER.toIcon())
        }
    }
    
}