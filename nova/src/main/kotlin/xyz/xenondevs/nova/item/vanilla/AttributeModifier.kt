package xyz.xenondevs.nova.item.vanilla

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.util.nmsAttribute
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.nmsOperation
import java.util.*
import org.bukkit.attribute.Attribute as BukkitAttribute
import org.bukkit.attribute.AttributeModifier.Operation as BukkitOperation
import org.bukkit.inventory.EquipmentSlot as BukkitEquipmentSlot

class AttributeModifier(
    val uuid: UUID,
    val name: String,
    val attribute: Attribute,
    val operation: Operation,
    val value: Double,
    val showInLore: Boolean,
    val slots: List<EquipmentSlot> = EquipmentSlot.entries
) {
    
    constructor(
        uuid: UUID,
        name: String,
        attribute: Attribute,
        operation: Operation,
        value: Double,
        showInLore: Boolean,
        vararg slots: EquipmentSlot
    ) : this(uuid, name, attribute, operation, value, showInLore, slots.asList())
    
    constructor(name: String, attribute: Attribute, operation: Operation, value: Double, showInLore: Boolean, vararg slots: EquipmentSlot) :
        this(UUID.nameUUIDFromBytes(name.toByteArray()), name, attribute, operation, value, showInLore, *slots)
    
    constructor(uuid: UUID, name: String, attribute: BukkitAttribute, operation: BukkitOperation, value: Double, showInLore: Boolean, vararg slots: BukkitEquipmentSlot) :
        this(uuid, name, attribute.nmsAttribute, operation.nmsOperation, value, showInLore, *slots.mapToArray { it.nmsEquipmentSlot })
    
    constructor(name: String, attribute: BukkitAttribute, operation: BukkitOperation, value: Double, showInLore: Boolean, vararg slots: BukkitEquipmentSlot) :
        this(UUID.nameUUIDFromBytes(name.toByteArray()), name, attribute.nmsAttribute, operation.nmsOperation, value, showInLore, *slots.mapToArray { it.nmsEquipmentSlot })
    
}
