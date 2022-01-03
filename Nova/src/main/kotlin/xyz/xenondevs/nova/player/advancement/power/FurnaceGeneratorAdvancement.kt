package xyz.xenondevs.nova.player.advancement.power

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.FURNACE_GENERATOR
import xyz.xenondevs.nova.player.advancement.RootAdvancement
import xyz.xenondevs.nova.player.advancement.addObtainCriteria
import xyz.xenondevs.nova.player.advancement.setDisplayLocalized
import xyz.xenondevs.nova.player.advancement.toIcon

object FurnaceGeneratorAdvancement : Advancement(NamespacedKey(NOVA, "furnace_generator")) {
    
    init {
        setParent(RootAdvancement.key)
        addObtainCriteria(FURNACE_GENERATOR)
        setDisplayLocalized {
            it.setIcon(FURNACE_GENERATOR.toIcon())
        }
    }
    
}