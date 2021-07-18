package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.cable.BasicCableAdvancement
import xyz.xenondevs.nova.material.NovaMaterial

object WirelessChargerAdvancement : Advancement(NamespacedKey(NOVA, "wireless_charger")) {
    
    init { 
        setParent(ChargerAdvancement.key)
        addObtainCriteria(NovaMaterial.WIRELESS_CHARGER)
        setDisplayLocalized {
            it.setIcon(NovaMaterial.WIRELESS_CHARGER.toIcon())
        }
    }
    
}