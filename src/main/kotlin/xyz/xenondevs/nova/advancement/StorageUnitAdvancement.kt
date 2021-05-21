package xyz.xenondevs.nova.advancement

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.cable.BasicCableAdvancement
import xyz.xenondevs.nova.material.NovaMaterial

object StorageUnitAdvancement : Advancement(NamespacedKey(NOVA, "storage_unit")) {
    
    init {
        setParent(BasicCableAdvancement.key)
        addObtainCriteria(NovaMaterial.STORAGE_UNIT)
        setDisplay {
            it.setTitle("Infinite Items")
            it.setDescription("Craft a Storage Unit")
            it.setIcon(NovaMaterial.STORAGE_UNIT.toIcon())
        }
    }
    
}