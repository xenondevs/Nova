package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import net.roxeez.advancement.trigger.TriggerType
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial

object RootAdvancement : Advancement(NamespacedKey(NOVA, "root")) {
    
    init {
        addCriteria("none", TriggerType.IMPOSSIBLE) {}
        setDisplay {
            it.setTitle("Nova")
            it.setDescription("")
            it.setIcon(NovaMaterial.IRON_GEAR.toIcon())
            it.setAnnounce(false)
            it.setToast(false)
            it.setBackground(NamespacedKey.minecraft("textures/block/tube_coral_block.png"))
        }
    }
    
}