package xyz.xenondevs.nova.util

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import kotlin.random.Random

fun Player.playItemPickupSound() {
    playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, Random.nextDouble(0.5, 0.7).toFloat())
}

fun Player.playClickSound() {
    playSound(location, Sound.UI_BUTTON_CLICK, 0.3f, 1f)
}

fun Location.playSoundNearby(sound: Sound, volume: Float, pitch: Float, vararg excluded: Player) =
    playSoundNearby(sound, SoundCategory.MASTER, volume, pitch, excluded = excluded)

fun Location.playSoundNearby(sound: Sound, category: SoundCategory, volume: Float, pitch: Float, vararg excluded: Player) =
    getPlayersNearby(if (volume > 1f) 16.0 * volume else 16.0, excluded = excluded)
        .forEach { it.playSound(this, sound, category, volume, pitch) }