package xyz.xenondevs.nova.world.hitbox

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.util.castRay
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
    
    @EventHandler(priority = EventPriority.LOWEST)
    fun handleInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND || event.isCompletelyDenied()) return
        
        val action = event.action
        if (action != Action.PHYSICAL) {
            val player = event.player
            
            var lastChunk: Chunk? = null
            var surroundingHitboxes: List<Hitbox>? = null
            player.eyeLocation.castRay(0.1, if (player.gameMode == GameMode.CREATIVE) 8.0 else 4.0) { location ->
                val block = location.block
                if (block.type.isTraversable() || !block.boundingBox.contains(location.x, location.y, location.z)) {
                    val chunk = block.chunk
                    
                    if (chunk != lastChunk) {
                        // if the ray has moved out of the chunk it was previously in, the surrounding hitboxes need to be recalculated
                        lastChunk = chunk
                        surroundingHitboxes = hitboxes[chunk]?.filter { it.checkQualify(event) } ?: emptyList()
                    }
                    
                    var continueRay = true
                    surroundingHitboxes!!.stream()
                        .filter { it.isInHitbox(location) && ProtectionManager.canUse(player, location) }
                        .findFirst()
                        .ifPresent {
                            continueRay = false
                            it.handleHit(event)
                        }
                    
                    return@castRay continueRay
                } else return@castRay false // block not traversable, don't continue ray
            }
        }
    }
    
    private fun Material.isTraversable() = isAir || name == "WATER" || name == "LAVA"
    
}