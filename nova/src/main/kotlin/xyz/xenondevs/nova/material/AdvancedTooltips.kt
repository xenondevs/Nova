package xyz.xenondevs.nova.material

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.persistentdata.get
import xyz.xenondevs.nova.data.serialization.persistentdata.set
import xyz.xenondevs.nova.util.registerEvents

private val ADVANCED_TOOLTIPS_KEY = NamespacedKey(NOVA, "advancedTooltips")

internal object AdvancedTooltips : Listener {
    
    private val _players = HashSet<Player>()
    val players: Set<Player>
        get() = _players
    
    init {
        registerEvents()
        Bukkit.getOnlinePlayers().forEach(::loadPlayer)
    }
    
    fun toggle(player: Player, state: Boolean): Boolean {
        val dataContainer = player.persistentDataContainer
        if (state) {
            if (player in _players)
                return false
            
            _players += player
            dataContainer.set(ADVANCED_TOOLTIPS_KEY, true)
            return true
        } else {
            if (player !in players)
                return false
            
            _players -= player
            dataContainer.set(ADVANCED_TOOLTIPS_KEY, false)
        }
        
        return true
    }
    
    private fun loadPlayer(player: Player) {
        if (player.persistentDataContainer.get<Boolean>(ADVANCED_TOOLTIPS_KEY) == true)
            _players += player
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        loadPlayer(event.player)
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        _players -= event.player
    }
    
}