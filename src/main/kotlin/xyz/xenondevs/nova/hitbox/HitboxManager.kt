package xyz.xenondevs.nova.hitbox

import com.sk89q.worldguard.protection.flags.Flags
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.WorldGuardUtils
import xyz.xenondevs.nova.util.castRay
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.isCompletelyDenied

object HitboxManager : Listener {
    
    private val hitboxes = HashMap<Chunk, ArrayList<Hitbox>>()
    
    init {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    fun addHitbox(hitbox: Hitbox) {
        val list = hitboxes[hitbox.chunk] ?: ArrayList<Hitbox>().also { hitboxes[hitbox.chunk] = it }
        list += hitbox
    }
    
    fun removeHitbox(hitbox: Hitbox) {
        val list = hitboxes[hitbox.chunk]
        if (list != null) {
            list -= hitbox
            if (list.isEmpty()) hitboxes -= hitbox.chunk
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun handleInteract(event: PlayerInteractEvent) {
        if (event.isCompletelyDenied()) return
        
        val action = event.action
        if (action != Action.PHYSICAL) {
            val player = event.player
            
            var lastChunk: Chunk? = null
            var surroundingHitboxes: List<Hitbox>? = null
            player.eyeLocation.castRay(0.1, 8.0) { location ->
                val block = location.block
                if (block.type.isTraversable() || !block.boundingBox.contains(location.x, location.y, location.z)) {
                    val chunk = block.chunk
                    if (chunk != lastChunk) {
                        // if the ray has moved out of the chunk it was previously in, the surrounding hitboxes need to be recalculated
                        lastChunk = chunk
                        val chunks = chunk.getSurroundingChunks(range = 1, includeCurrent = true)
                        surroundingHitboxes = chunks.flatMap {
                            hitboxes[it] ?: emptyList()
                        }.filter { it.checkQualify(event) }
                    }
                    
                    val hitHitboxes = surroundingHitboxes!!.filter { it.isInHitbox(location) }
                    if (hitHitboxes.isNotEmpty()) {
                        if (WorldGuardUtils.runQuery(player, location, Flags.USE))
                            hitHitboxes.forEach { it.handleHit(event) }
                        return@castRay false // don't continue ray
                    }
                } else {
                    return@castRay false // don't continue ray
                }
                
                return@castRay true // continue ray
            }
        }
    }
    
    private fun Material.isTraversable() = isAir || name == "WATER" || name == "LAVA"
    
}