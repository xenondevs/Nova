package xyz.xenondevs.nova.util

import org.bukkit.Sound
import org.bukkit.entity.Player
import kotlin.random.Random

fun Player.playItemPickupSound() {
    playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, Random.nextDouble(0.5, 0.7).toFloat())
}

fun Player.playClickSound() {
    playSound(location, Sound.UI_BUTTON_CLICK, 0.5f, 1f)
}