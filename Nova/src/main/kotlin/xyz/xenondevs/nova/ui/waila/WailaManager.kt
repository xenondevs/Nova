package xyz.xenondevs.nova.ui.waila

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.pos

internal object WailaManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = emptySet<Initializable>()
    
    private val overlays = HashMap<Player, Waila>()
    
    override fun init() {
        registerEvents()
        Bukkit.getOnlinePlayers().forEach(::addWailaOverlay)
    }
    
    override fun disable() {
        overlays.values.forEach { it.setActive(false) }
    }
    
    private fun addWailaOverlay(player: Player) {
        overlays[player] = Waila(player)
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        addWailaOverlay(event.player)
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        overlays.remove(event.player)?.setActive(false)
    }
    
    @EventHandler
    private fun handleMove(event: PlayerMoveEvent) {
        val lookingAt = event.player.getTargetBlockExact(4)?.pos
        overlays[event.player]?.update(lookingAt)
    }
    
}