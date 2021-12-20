package xyz.xenondevs.nova.player.advancement.agriculture

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.TREE_FACTORY
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object TreeFactoryAdvancement : Advancement(NamespacedKey(NOVA, "tree_factory")) {
    
    init {
        setParent(HarvesterAdvancement.key)
        addObtainCriteria(TREE_FACTORY)
        setDisplayLocalized {
            it.setIcon(TREE_FACTORY.toIcon())
        }
    }
    
}