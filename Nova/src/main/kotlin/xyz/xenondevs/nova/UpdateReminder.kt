package xyz.xenondevs.nova

import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.runAsyncTaskTimer
import java.net.URL

object UpdateReminder : Listener {
    
    @Volatile
    private var needsUpdate = false
    private var taskId: Int = -1
    
    fun init() {
        if (NOVA.devBuild) return
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        taskId = runAsyncTaskTimer(0, 12000) {
            checkVersion()
            if (needsUpdate) {
                val sender = Bukkit.getConsoleSender()
                sender.sendMessage("§cYou're running an outdated version of §bNova§c.")
                sender.sendMessage("§cPlease download the latest version at §bhttps://spigotmc.org/resources/93648§c.")
            }
        }.taskId
    }
    
    private fun checkVersion() {
        if (needsUpdate) return
        val newVersion = Version(URL("https://api.spigotmc.org/legacy/update.php?resource=93648").readText())
        if (newVersion > NOVA.version) {
            needsUpdate = true
            if (taskId != -1) {
                Bukkit.getScheduler().cancelTask(taskId)
                taskId = -1
            }
        }
    }
    
    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPermission("nova.misc.updateReminder") && needsUpdate) {
            val message = ComponentBuilder()
                .append(TranslatableComponent("nova.outdated_version"))
                .appendLegacy("§bhttps://spigotmc.org/resources/93648")
                .create()
            player.spigot().sendMessage(*message)
        }
    }
    
}