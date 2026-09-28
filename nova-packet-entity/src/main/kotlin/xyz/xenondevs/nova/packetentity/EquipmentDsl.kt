package xyz.xenondevs.nova.packetentity

import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.dsl.DslProperty

/**
 * DSL property for configuring equipment slots.
 *
 * Equipment slots can be selected dynamically:
 * ```kotlin
 * equipment[EquipmentSlot.HEAD] by helmet
 * ```
 */
@PacketEntityDslMarker
sealed interface EquipmentDslProperty {
    
    /**
     * Gets the DSL property for [slot].
     */
    operator fun get(slot: EquipmentSlot): DslProperty<ItemStack?>
    
}

/**
 * DSL scope for configuring packet entity equipment.
 *
 * ```kotlin
 * equipment {
 *     hand by sword
 *     head by helmet
 * }
 * ```
 */
@PacketEntityDslMarker
sealed interface EquipmentDsl {
    
    /** The main-hand item. */
    val hand: DslProperty<ItemStack?>
    
    /** The off-hand item. */
    val offHand: DslProperty<ItemStack?>
    
    /** The boots item. */
    val feet: DslProperty<ItemStack?>
    
    /** The leggings item. */
    val legs: DslProperty<ItemStack?>
    
    /** The chestplate item. */
    val chest: DslProperty<ItemStack?>
    
    /** The helmet item. */
    val head: DslProperty<ItemStack?>
    
    /** The body equipment item. */
    val body: DslProperty<ItemStack?>
    
    /** The saddle item. */
    val saddle: DslProperty<ItemStack?>
    
}

/**
 * Mutable equipment API for packet entities.
 *
 * Equipment can be accessed by Bukkit [EquipmentSlot] or by named properties:
 * ```kotlin
 * entity.equipment[EquipmentSlot.HEAD] = helmet
 * entity.equipment.hand = sword
 * ```
 */
@PacketEntityDslMarker
sealed interface PacketEntityEquipment {
    
    /** Gets the item currently configured for [slot]. */
    operator fun get(slot: EquipmentSlot): ItemStack?
    
    /** Sets the item for [slot]. */
    operator fun set(slot: EquipmentSlot, value: ItemStack?)
    
    /** The main-hand item. */
    var hand: ItemStack?
    
    /** The off-hand item. */
    var offHand: ItemStack?
    
    /** The boots item. */
    var feet: ItemStack?
    
    /** The leggings item. */
    var legs: ItemStack?
    
    /** The chestplate item. */
    var chest: ItemStack?
    
    /** The helmet item. */
    var head: ItemStack?
    
    /** The body equipment item. */
    var body: ItemStack?
    
    /** The saddle item. */
    var saddle: ItemStack?
    
}
