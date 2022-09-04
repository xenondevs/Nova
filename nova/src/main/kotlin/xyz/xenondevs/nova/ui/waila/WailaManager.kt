package xyz.xenondevs.nova.ui.waila

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.serialization.persistentdata.get
import xyz.xenondevs.nova.data.serialization.persistentdata.set
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.unregisterEvents

private val WAILA_ENABLED_KEY = NamespacedKey(NOVA, "waila")
private val UPDATE_INTERVAL by configReloadable { DEFAULT_CONFIG.getLong("waila.update_interval") }

private val Player.isWailaEnabled: Boolean
    get() = persistentDataContainer.get<Boolean>(WAILA_ENABLED_KEY) != false

internal object WailaManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = setOf(Resources, AddonsInitializer)
    
    private var tickTask: BukkitTask? = null
    private val overlays = HashMap<Player, Waila>()
    
    override fun init() {
        reload()
    }
    
    fun reload() {
        unregisterEvents()
        overlays.values.forEach { it.setActive(false) }
        overlays.clear()
        tickTask?.cancel()
        if (DEFAULT_CONFIG.getBoolean("waila.enabled")) {
            registerEvents()
            Bukkit.getOnlinePlayers().forEach(::tryAddWailaOverlay)
            tickTask = runTaskTimer(0, UPDATE_INTERVAL) { overlays.values.forEach(Waila::handleTick) }
        }
    }
    
    fun toggle(player: Player, state: Boolean): Boolean {
        val dataContainer = player.persistentDataContainer
        if (state) {
            if (player in overlays)
                return false
            
            dataContainer.set(WAILA_ENABLED_KEY, true)
            addWailaOverlay(player)
        } else {
            if (player !in overlays)
                return false
            
            dataContainer.set(WAILA_ENABLED_KEY, false)
            removeWailaOverlay(player)
        }
        
        return true
    }
    
    override fun disable() {
        overlays.values.forEach { it.setActive(false) }
    }
    
    private fun tryAddWailaOverlay(player: Player) {
        if (player.isWailaEnabled)
            addWailaOverlay(player)
    }
    
    private fun addWailaOverlay(player: Player) {
        overlays[player] = Waila(player)
    }
    
    private fun removeWailaOverlay(player: Player) {
        overlays.remove(player)?.setActive(false)
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        tryAddWailaOverlay(event.player)
    }
    
    @EventHandler
    private fun handleQuit(event: PlayerQuitEvent) {
        removeWailaOverlay(event.player)
    }
    
}