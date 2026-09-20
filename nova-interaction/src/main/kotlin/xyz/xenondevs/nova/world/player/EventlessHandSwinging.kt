package xyz.xenondevs.nova.world.player

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.EquipmentSlot

/**
 * Lets the [player][this] swing their main hand without triggering any server-side
 * interaction related events.
 */
@Deprecated("Swing animations no longer cause a client packet; use swingMainHand()", ReplaceWith("swingMainHand()"))
fun LivingEntity.swingMainHandEventless() = swingMainHand()

/**
 * Lets the [player][this] swing their off-hand without triggering any server-side
 * interaction related events.
 */
@Deprecated("Swing animations no longer cause a client packet; use swingOffHand()", ReplaceWith("swingOffHand()"))
fun LivingEntity.swingOffHandEventless() = swingOffHand()

/**
 * Lets the [player][this] swing their [hand] without triggering any server-side
 * interaction related events.
 *
 * @throws IllegalArgumentException if the [hand] is not [EquipmentSlot.HAND] or [EquipmentSlot.OFF_HAND]
 */
@Deprecated("Swing animations no longer cause a client packet; use swingHand(hand)", ReplaceWith("swingHand(hand)"))
fun LivingEntity.swingHandEventless(hand: EquipmentSlot) = swingHand(hand)
