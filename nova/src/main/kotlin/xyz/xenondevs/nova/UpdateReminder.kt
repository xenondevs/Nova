package xyz.xenondevs.nova

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.util.component.adventure.sendMessage
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runAsyncTaskTimer
import java.net.URL

internal object UpdateReminder : Initializable(), Listener {
    
    private const val NOVA_RESOURCE_ID = 93648
    
    override val initializationStage = InitializationStage.POST_WORLD_ASYNC
    override val dependsOn = setOf(NovaConfig)
    
    private var task: BukkitTask? = null
    private val needsUpdate = ArrayList<Addon?>()
    private val alreadyNotified = ArrayList<Addon?>()
    
    override fun init() {
        reload()
    }
    
    fun reload() {
        val enabled = DEFAULT_CONFIG.getBoolean("update_reminder.enabled")
        if (task == null && enabled) {
            enableReminder()
        } else if (task != null && !enabled) {
            disableReminder()
        }
    }
    
    private fun enableReminder() {
        registerEvents()
        
        task = runAsyncTaskTimer(0, DEFAULT_CONFIG.getLong("update_reminder.interval")) {
            checkVersions()
            if (needsUpdate.isNotEmpty()) {
                needsUpdate.asSequence().filter { it !in alreadyNotified }.forEach {
                    val name = it?.description?.name ?: "Nova"
                    val id = it?.description?.spigotResourceId ?: NOVA_RESOURCE_ID
                    LOGGER.warning("You're running an outdated version of $name. " +
                        "Please download the latest version at https://spigotmc.org/resources/$id")
                    alreadyNotified += it
                }
            }
        }
    }
    
    private fun disableReminder() {
        HandlerList.unregisterAll(this)
        
        task?.cancel()
        task = null
    }
    
    private fun checkVersions() {
        checkVersion(null)
        AddonManager.addons.values
            .filter { it.description.spigotResourceId != -1 }
            .forEach(::checkVersion)
    }
    
    private fun checkVersion(addon: Addon?) {
        if (addon in needsUpdate)
            return
        
        val id: Int
        val currentVersion: Version
        
        if (addon != null) {
            id = addon.description.spigotResourceId
            currentVersion = Version(addon.description.version)
        } else {
            id = NOVA_RESOURCE_ID
            currentVersion = NOVA.version
        }
        
        if (id == -1)
            return
        
        try {
            val newVersion = Version(URL("https://api.spigotmc.org/legacy/update.php?resource=$id").readText())
            if (newVersion > currentVersion)
                needsUpdate += addon
        } catch (ignored: Throwable) {
            LOGGER.warning("Failed to connect to SpigotMC while trying to check for updates")
        }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPermission("nova.misc.updateReminder") && needsUpdate.isNotEmpty()) {
            needsUpdate.forEach { player.sendMessage(getOutdatedMessage(it)) }
        }
    }
    
    private fun getOutdatedMessage(addon: Addon?): Component {
        val name = addon?.description?.name ?: "Nova"
        val url = "https://spigotmc.org/resources/" + (addon?.description?.spigotResourceId ?: NOVA_RESOURCE_ID)
        
        return Component.translatable(
            "nova.outdated_version",
            NamedTextColor.RED,
            Component.text(name, NamedTextColor.AQUA),
            Component.text(url).clickEvent(ClickEvent.openUrl(url))
        )
    }
    
}