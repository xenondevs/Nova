package xyz.xenondevs.nova.item.vanilla

import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.attribute.Attribute
import java.util.*

class AttributeModifier constructor(
    val uuid: UUID,
    val name: String,
    val attribute: Attribute,
    val operation: Operation,
    val value: Double,
    vararg slots: EquipmentSlot
) {
    
    val slots = if (slots.isEmpty()) EquipmentSlot.values() else slots
    
    constructor(name: String, attribute: Attribute, operation: Operation, value: Double, vararg slots: EquipmentSlot) :
        this(UUID.nameUUIDFromBytes(name.toByteArray()), name, attribute, operation, value, *slots)
    
    constructor(attribute: Attribute, operation: Operation, value: Double, vararg slots: EquipmentSlot) :
        this(UUID.randomUUID(), "", attribute, operation, value, *slots)
    
    enum class Operation {
        INCREMENT,
        MULTIPLY,
        MULTIPLY_BASE
    }
    
}
