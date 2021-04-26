package xyz.xenondevs.nova.hitbox

import org.bukkit.Location
import org.bukkit.event.player.PlayerInteractEvent

class Hitbox(
    private val from: Location,
    private val to: Location,
    val checkQualify: (PlayerInteractEvent) -> Boolean = { true },
    val handleHit: (PlayerInteractEvent) -> Unit
) {
    
    val chunk = from.chunk
    
    init {
        HitboxManager.addHitbox(this)
    }
    
    fun isInHitbox(location: Location): Boolean {
        return location.x in from.x..to.x
            && location.y in from.y..to.y
            && location.z in from.z..to.z
    }
    
    fun remove() {
        HitboxManager.removeHitbox(this)
    }
    
}