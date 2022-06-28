package xyz.xenondevs.nova.world.block.hitbox

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.util.castRay
import xyz.xenondevs.nova.util.isCompletelyDenied
import xyz.xenondevs.nova.util.item.isTraversable
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.chunkPos

object HitboxManager : Listener {
    
    private val hitboxes = HashMap<ChunkPos, ArrayList<Hitbox>>()
    
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
    fun handleInteract(e: WrappedPlayerInteractEvent) {
        val event = e.event
        if (event.hand != EquipmentSlot.HAND || event.isCompletelyDenied()) return
        
        val action = event.action
        if (action != Action.PHYSICAL) {
            val player = event.player
            
            var lastChunk: ChunkPos? = null
            var surroundingHitboxes: List<Hitbox>? = null
            player.eyeLocation.castRay(0.1, if (player.gameMode == GameMode.CREATIVE) 8.0 else 4.0) { location ->
                val block = location.block
                if (block.type.isTraversable() || !block.boundingBox.contains(location.x, location.y, location.z)) {
                    val chunk = block.chunkPos
                    
                    if (chunk != lastChunk) {
                        // if the ray has moved out of the chunk it was previously in, the surrounding hitboxes need to be recalculated
                        lastChunk = chunk
                        surroundingHitboxes = hitboxes[chunk]?.filter { it.checkQualify(event) } ?: emptyList()
                    }
                    
                    var continueRay = true
                    surroundingHitboxes!!.asSequence()
                        .filter { it.isInHitbox(location) }
                        .firstOrNull()
                        ?.also {
                            continueRay = false
                            
                            if (ProtectionManager.canUseBlock(player, event.item, location).get())
                                it.handleHit(event)
                        }
                    
                    return@castRay continueRay
                } else return@castRay false // block not traversable, don't continue ray
            }
        }
    }
    
}