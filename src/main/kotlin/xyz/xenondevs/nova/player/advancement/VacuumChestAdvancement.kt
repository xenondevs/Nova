package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.VACUUM_CHEST

object VacuumChestAdvancement : Advancement(NamespacedKey(NOVA, "vacuum_chest")) {
    
    init {
        setParent(ItemFilterAdvancement.key)
        addObtainCriteria(VACUUM_CHEST)
        setDisplayLocalized {
            it.setIcon(VACUUM_CHEST.toIcon())
        }
    }
    
}