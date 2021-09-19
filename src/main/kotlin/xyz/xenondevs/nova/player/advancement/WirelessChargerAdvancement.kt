package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.WIRELESS_CHARGER

object WirelessChargerAdvancement : Advancement(NamespacedKey(NOVA, "wireless_charger")) {
    
    init {
        setParent(ChargerAdvancement.key)
        addObtainCriteria(WIRELESS_CHARGER)
        setDisplayLocalized {
            it.setIcon(WIRELESS_CHARGER.toIcon())
        }
    }
    
}