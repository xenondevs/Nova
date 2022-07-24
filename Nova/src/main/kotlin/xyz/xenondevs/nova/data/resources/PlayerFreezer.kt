package xyz.xenondevs.nova.data.resources

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.player.*
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status.*
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.util.removeIf

internal object PlayerFreezer : Listener {
    
    var enabled = DEFAULT_CONFIG.getBoolean("resource_pack.freeze_loading_players")
    
    val frozenPlayers = HashMap<Player, Boolean>() // Player -> prevAllowFlight
    
    fun clearPlayers() {
        frozenPlayers.removeIf {
            it.key.allowFlight = it.value
            true
        }
    }
    
    @EventHandler
    private fun handleResourcePackStatus(event: PlayerResourcePackStatusEvent) {
        if (event.status == ACCEPTED) {
            frozenPlayers[event.player] = event.player.allowFlight
            event.player.allowFlight = true
        } else if (event.status == SUCCESSFULLY_LOADED || event.status == FAILED_DOWNLOAD) {
            frozenPlayers[event.player]?.let {
                event.player.allowFlight = it
                frozenPlayers -= event.player
            }
        }
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        val player = event.player
        frozenPlayers[player]?.let {
            player.allowFlight = it
            frozenPlayers -= player
        }
    }
    
    @EventHandler
    private fun handleMove(event: PlayerMoveEvent) {
        if (event.player in frozenPlayers)
            event.isCancelled = true
    }
    
    @EventHandler
    private fun handleDamage(event: EntityDamageEvent) {
        if (event.entity is Player && event.entity in frozenPlayers)
            event.isCancelled = true
    }
    
    @EventHandler
    private fun handleDamageByPlayer(event: EntityDamageByEntityEvent) {
        if (event.damager != event.entity && event.damager is Player && event.damager in frozenPlayers)
            event.isCancelled = true
    }
    
    @EventHandler
    private fun handleInteract(event: PlayerInteractEvent) {
        if (event.player in frozenPlayers)
            event.isCancelled = true
    }
    
    @EventHandler
    private fun handleEntityInteract(event: PlayerInteractEntityEvent) {
        if (event.player in frozenPlayers)
            event.isCancelled = true
    }
    
    @EventHandler
    private fun handleInventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked is Player && event.whoClicked in frozenPlayers)
            event.isCancelled = true
    }
    
    @EventHandler
    private fun handleDrop(event: PlayerDropItemEvent) {
        if (event.player in frozenPlayers)
            event.isCancelled = true
    }
    
    @EventHandler
    private fun handleCreative(event: InventoryCreativeEvent) {
        if (event.whoClicked is Player && event.whoClicked in frozenPlayers)
            event.isCancelled = true
    }
    
}