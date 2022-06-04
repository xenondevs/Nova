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

private val ADVANCED_TOOLTIPS_KEY = NamespacedKey(NOVA, "advancedTooltips")

internal object AdvancedTooltips : Listener {
    
    private val _players = HashSet<Player>()
    val players: Set<Player>
        get() = _players
    
    init {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        Bukkit.getOnlinePlayers().forEach(::loadPlayer)
    }
    
    fun toggle(player: Player): Boolean {
        val dataContainer = player.persistentDataContainer
        if (player in _players) {
            _players -= player
            dataContainer.set(ADVANCED_TOOLTIPS_KEY, false)
        } else {
            _players += player
            dataContainer.set(ADVANCED_TOOLTIPS_KEY, true)
        }
        
        return player in _players
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