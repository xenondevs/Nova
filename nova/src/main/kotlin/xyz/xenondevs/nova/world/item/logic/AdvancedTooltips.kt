package xyz.xenondevs.nova.world.item.logic

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.util.PlayerMapManager
import xyz.xenondevs.nova.util.registerEvents

private val ADVANCED_TOOLTIPS_KEY = NamespacedKey("nova", "has_advanced_tooltips")

internal object AdvancedTooltips : Listener {
    
    private val players = PlayerMapManager.createConcurrentSet()
    
    init {
        registerEvents()
        Bukkit.getOnlinePlayers().forEach(AdvancedTooltips::loadPlayer)
    }
    
    operator fun set(player: Player, state: Boolean): Boolean {
        player.persistentDataContainer[ADVANCED_TOOLTIPS_KEY, PersistentDataType.BOOLEAN] = state
        return if (state) {
            players.add(player)
        } else {
            players.remove(player)
        }
    }
    
    operator fun get(player: Player): Boolean =
        player in players
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        loadPlayer(event.player)
    }
    
    private fun loadPlayer(player: Player) {
        if (player.persistentDataContainer[ADVANCED_TOOLTIPS_KEY, PersistentDataType.BOOLEAN] == true)
            players += player
    }
    
}