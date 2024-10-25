package xyz.xenondevs.nova.world.item.logic

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.serialization.persistentdata.get
import xyz.xenondevs.nova.serialization.persistentdata.set
import xyz.xenondevs.nova.util.registerEvents

private val ADVANCED_TOOLTIPS_KEY = NamespacedKey(Nova, "advancedTooltipsType")

internal object AdvancedTooltips : Listener {
    
    private val players = HashMap<Player, Type>()
    
    init {
        registerEvents()
        Bukkit.getOnlinePlayers().forEach(AdvancedTooltips::loadPlayer)
    }
    
    fun setType(player: Player, type: Type): Boolean {
        val dataContainer = player.persistentDataContainer
        val current = players[player] ?: Type.OFF
        
        if (current == type)
            return false
        
        players[player] = type
        dataContainer.set(ADVANCED_TOOLTIPS_KEY, type)
        
        return true
    }
    
    fun hasNovaTooltips(player: Player): Boolean =
        players[player]?.includesNova == true
    
    fun hasVanillaTooltips(player: Player): Boolean =
        players[player]?.includesVanilla == true
    
    private fun loadPlayer(player: Player) {
        players[player] = player.persistentDataContainer.get(ADVANCED_TOOLTIPS_KEY) ?: Type.OFF
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        loadPlayer(event.player)
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        players -= event.player
    }
    
    enum class Type(val includesNova: Boolean, val includesVanilla: Boolean) {
        OFF(false, false),
        NOVA(true, false),
        ALL(true, true)
    }
    
}