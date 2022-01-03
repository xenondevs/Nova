package xyz.xenondevs.nova.util

import org.bukkit.event.Event.Result
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

fun Action.isRightClick() = this == Action.RIGHT_CLICK_BLOCK || this == Action.RIGHT_CLICK_AIR

fun Action.isLeftClick() = this == Action.LEFT_CLICK_BLOCK || this == Action.LEFT_CLICK_AIR

fun PlayerInteractEvent.isCompletelyDenied() = useInteractedBlock() == Result.DENY && useItemInHand() == Result.DENY

val PlayerInteractEvent.handItems: Array<ItemStack>
    get() = arrayOf(player.inventory.itemInMainHand, player.inventory.itemInOffHand)

val PlayerInteractEvent.hands: Array<Pair<EquipmentSlot, ItemStack>>
    get() = arrayOf(EquipmentSlot.HAND to player.inventory.itemInMainHand, EquipmentSlot.OFF_HAND to player.inventory.itemInOffHand)