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
    val slot: EquipmentSlot? = null
) {
    
    constructor(name: String, attribute: Attribute, operation: Operation, value: Double, slot: EquipmentSlot? = null) :
        this(UUID.nameUUIDFromBytes(name.toByteArray()), name, attribute, operation, value, slot)
    
    constructor(attribute: Attribute, operation: Operation, value: Double, slot: EquipmentSlot? = null) :
        this(UUID.randomUUID(), "", attribute, operation, value, slot)
    
    enum class Operation {
        INCREMENT,
        MULTIPLY,
        MULTIPLY_BASE
    }
    
}
