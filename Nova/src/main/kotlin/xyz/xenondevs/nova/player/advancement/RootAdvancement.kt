package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import net.roxeez.advancement.trigger.TriggerType
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.COPPER_GEAR

object RootAdvancement : Advancement(NamespacedKey(NOVA, "root")) {
    
    init {
        addCriteria("none", TriggerType.IMPOSSIBLE) {}
        setDisplayLocalized {
            it.setIcon(COPPER_GEAR.toIcon())
            it.setAnnounce(false)
            it.setToast(false)
            it.setBackground(NamespacedKey.minecraft("textures/block/tube_coral_block.png"))
        }
    }
    
}