package xyz.xenondevs.nova.item.vanilla

import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.attribute.Attribute
import java.util.*

class AttributeModifier constructor(
    val attribute: Attribute,
    val operation: Operation,
    val value: Double,
    val slot: EquipmentSlot? = null
) {
    
    val uuid = UUID.randomUUID()
    
    enum class Operation {
        INCREMENT,
        MULTIPLY,
        MULTIPLY_BASE
    }
    
}
