package xyz.xenondevs.nova.ui.waila

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.serialization.persistentdata.get
import xyz.xenondevs.nova.serialization.persistentdata.set
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.unregisterEvents
import xyz.xenondevs.nova.api.player.WailaManager as IWailaManager

private val WAILA_ENABLED_KEY = NamespacedKey("nova", "waila")

private val Player.isWailaEnabled: Boolean
    get() = persistentDataContainer.get<Boolean>(WAILA_ENABLED_KEY) != false

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [ResourceGeneration.PostWorld::class]
)
internal object WailaManager : Listener, IWailaManager {
    
    private val ENABLED_PROVIDER = MAIN_CONFIG.entry<Boolean>("waila", "enabled")
    val ENABLED by ENABLED_PROVIDER
    
    private var tickTask: BukkitTask? = null
    private val overlays = HashMap<Player, Waila>()
    
    //<editor-fold desc="Nova-API", defaultstate="collapsed">
    override fun isCompletelyDisabled(): Boolean = !ENABLED
    override fun getState(player: Player): Boolean = player.isWailaEnabled
    override fun setState(player: Player, enabled: Boolean): Boolean = if (toggle(player, enabled)) enabled else !enabled
    //</editor-fold>
    
    @InitFun
    private fun init() {
        ENABLED_PROVIDER.subscribe(::reload)
        reload(ENABLED)
    }
    
    private fun reload(enabled: Boolean) {
        unregisterEvents()
        overlays.values.forEach { it.setActive(false) }
        overlays.clear()
        tickTask?.cancel()
        if (enabled) {
            registerEvents()
            Bukkit.getOnlinePlayers().forEach(::tryAddWailaOverlay)
            tickTask = runTaskTimer(0, 1) { overlays.values.forEach(Waila::handleTick) }
        }
    }
    
    @DisableFun
    private fun disable() {
        overlays.values.forEach { it.setActive(false) }
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