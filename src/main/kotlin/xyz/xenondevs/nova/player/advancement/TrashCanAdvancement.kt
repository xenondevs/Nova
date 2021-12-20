package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.player.advancement.cable.BasicCableAdvancement

object TrashCanAdvancement : Advancement(NamespacedKey(NOVA, "trash_can")) {
    
    init {
        setParent(BasicCableAdvancement.key)
        addObtainCriteria(NovaMaterialRegistry.TRASH_CAN)
        setDisplayLocalized {
            it.setIcon(NovaMaterialRegistry.TRASH_CAN.toIcon())
        }
    }
    
}