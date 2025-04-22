package xyz.xenondevs.nova.update

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.commons.version.Version
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA_VERSION
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.name
import xyz.xenondevs.nova.addon.version
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.strongNode
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.AsyncExecutor
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.unregisterEvents

private val NOVA_DISTRIBUTORS = listOf(
    // GitHub is intentionally omitted because in our current setup releases are created before the jar is uploaded
    ProjectDistributor.hangar("xenondevs/Nova"),
    ProjectDistributor.modrinth("nova-framework")
)

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC
)
internal object UpdateReminder : Listener {
    
    private var job: Job? = null
    private val needsUpdate = HashMap<Addon?, String>()
    private val alreadyNotified = ArrayList<Addon?>()
    
    @InitFun
    private fun init() {
        val cfg = MAIN_CONFIG.strongNode("update_reminder")
        cfg.subscribe(::reload)
        reload(cfg.get())
    }
    
    private fun reload(cfg: ConfigurationNode) {
        val enabled = cfg.node("enabled").boolean
        val interval = cfg.node("interval").long
        
        if (job == null && enabled) {
            // Enable reminder
            registerEvents()
            job = CoroutineScope(AsyncExecutor.SUPERVISOR).launch {
                checkForUpdates()
                delay(interval)
            }
        } else if (job != null && !enabled) {
            // Disable reminder
            unregisterEvents()
            job?.cancel()
            job = null
        }
    }
    
    private suspend fun checkForUpdates() {
        checkVersion(null)
        AddonBootstrapper.addons
            .filter { it.projectDistributors.isNotEmpty() }
            .forEach { checkVersion(it) }
        
        if (needsUpdate.isNotEmpty()) {
            needsUpdate.asSequence()
                .filter { it.key !in alreadyNotified }
                .forEach { (addon, resourcePage) ->
                    val name = addon?.name ?: "Nova"
                    LOGGER.warn("You're running an outdated version of $name. Please download the latest version at $resourcePage")
                    alreadyNotified += addon
                }
        }
    }
    
    private suspend fun checkVersion(addon: Addon?) {
        if (addon in needsUpdate)
            return
        
        val distributors: List<ProjectDistributor>
        val currentVersion: Version
        
        if (addon != null) {
            distributors = addon.projectDistributors
            currentVersion = Version(addon.version)
        } else {
            distributors = NOVA_DISTRIBUTORS
            currentVersion = NOVA_VERSION
        }
        
        for (distributor in distributors) {
            try {
                val newVersion = distributor.getLatestVersion(currentVersion.isFullRelease)
                if (newVersion > currentVersion) {
                    needsUpdate[addon] = distributor.projectUrl
                    break
                }
            } catch (_: Throwable) {
            }
        }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPermission("nova.misc.updateReminder") && needsUpdate.isNotEmpty()) {
            needsUpdate.forEach { (addon, resourcePage) ->
                val name = addon?.name ?: "Nova"
                val msg = Component.translatable(
                    "nova.outdated_version",
                    NamedTextColor.RED,
                    Component.text(name, NamedTextColor.AQUA),
                    Component.text(resourcePage).clickEvent(ClickEvent.openUrl(resourcePage))
                )
                player.sendMessage(msg)
            }
        }
    }
    
}