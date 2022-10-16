package xyz.xenondevs.nova.util

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import xyz.xenondevs.nova.world.BlockPos
import kotlin.random.Random

fun Player.playItemPickupSound() {
    playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, Random.nextDouble(0.5, 0.7).toFloat())
}

fun Player.playClickSound() {
    playSound(location, Sound.UI_BUTTON_CLICK, 0.5f, 1f)
}

@Deprecated("Unnecessary maxDistance parameter", ReplaceWith("playSoundNearby(sound, volume, pitch, excluded)"))
fun Location.playSoundNearby(maxDistance: Double, sound: Sound, volume: Float, pitch: Float, vararg excluded: Player) =
    getPlayersNearby(maxDistance, excluded = excluded)
        .forEach { it.playSound(this, sound, volume, pitch) }

fun Location.playSoundNearby(sound: Sound, volume: Float, pitch: Float, vararg excluded: Player) =
    playSoundNearby(sound, SoundCategory.MASTER, volume, pitch, excluded = excluded)

fun Location.playSoundNearby(sound: Sound, category: SoundCategory, volume: Float, pitch: Float, vararg excluded: Player) =
    getPlayersNearby(if (volume > 1f) 16.0 * volume else 16.0, excluded = excluded)
        .forEach { it.playSound(this, sound, category, volume, pitch) }

class SoundEffect(private val sound: String, category: SoundCategory? = null) {
    
    private val category = category ?: SoundCategory.MASTER
    
    constructor(sound: Sound, category: SoundCategory? = null) : this(sound.key.toString(), category)
    
    fun play(location: Location) {
        location.world!!.playSound(location, sound, category, 1f, Random.nextDouble(0.8, 0.95).toFloat())
    }
    
    fun play(pos: BlockPos) = play(pos.location.add(0.5, 0.5, 0.5))
    
}