package xyz.xenondevs.nova.util

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import kotlin.random.Random

/**
 * Plays the ui button click sound for the player.
 */
fun Player.playClickSound() {
    playSound(location, Sound.UI_BUTTON_CLICK, 0.25f, 1f)
}

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, Random.nextDouble(0.5, 0.7).toFloat())", "org.bukkit.Sound", "kotlin.random.Random"))
fun Player.playItemPickupSound() {
    playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, Random.nextDouble(0.5, 0.7).toFloat())
}

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("playSoundNearby(sound, SoundCategory.MASTER, volume, pitch, excluded = excluded)", "org.bukkit.SoundCategory"))
fun Location.playSoundNearby(sound: Sound, volume: Float, pitch: Float, vararg excluded: Player) =
    playSoundNearby(sound, SoundCategory.MASTER, volume, pitch, excluded = excluded)

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("getPlayersNearby(if (volume > 1f) 16.0 * volume else 16.0, excluded = excluded).forEach { it.playSound(this, sound, category, volume, pitch) }"))
fun Location.playSoundNearby(sound: Sound, category: SoundCategory, volume: Float, pitch: Float, vararg excluded: Player) =
    getPlayersNearby(if (volume > 1f) 16.0 * volume else 16.0, excluded = excluded)
        .forEach { it.playSound(this, sound, category, volume, pitch) }