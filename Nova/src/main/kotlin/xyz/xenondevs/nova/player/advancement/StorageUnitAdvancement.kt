package xyz.xenondevs.nova.player.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterialRegistry.STORAGE_UNIT

object StorageUnitAdvancement : Advancement(NamespacedKey(NOVA, "storage_unit")) {
    
    init {
        setParent(VacuumChestAdvancement.key)
        addObtainCriteria(STORAGE_UNIT)
        setDisplayLocalized {
            it.setIcon(STORAGE_UNIT.toIcon())
        }
    }
    
}