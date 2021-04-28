package xyz.xenondevs.nova.util

import org.bukkit.event.Event.Result
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

fun Action.isRightClick() = this == Action.RIGHT_CLICK_BLOCK || this == Action.RIGHT_CLICK_AIR

fun Action.isLeftClick() = this == Action.LEFT_CLICK_BLOCK || this == Action.LEFT_CLICK_AIR

fun PlayerInteractEvent.isCompletelyDenied() = useInteractedBlock() == Result.DENY && useItemInHand() == Result.DENY
