package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ITEM_FILTER
import xyz.xenondevs.nova.player.advancement.cable.BasicCableAdvancement

object ItemFilterAdvancement : Advancement(NamespacedKey(NOVA, "item_filter")) {
    
    init {
        setParent(BasicCableAdvancement.key)
        addObtainCriteria(ITEM_FILTER)
        setDisplayLocalized {
            it.setIcon(ITEM_FILTER.toIcon())
        }
    }
    
}