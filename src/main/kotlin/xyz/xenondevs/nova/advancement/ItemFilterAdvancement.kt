package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.cable.BasicCableAdvancement
import xyz.xenondevs.nova.material.NovaMaterial

object ItemFilterAdvancement : Advancement(NamespacedKey(NOVA, "item_filter")) {
    
    init {
        setParent(BasicCableAdvancement.key)
        addObtainCriteria(NovaMaterial.ITEM_FILTER)
        setDisplay {
            it.setTitle("Filtering")
            it.setDescription("Craft an Item Filter")
            it.setIcon(NovaMaterial.ITEM_FILTER.toIcon())
        }
    }
    
}