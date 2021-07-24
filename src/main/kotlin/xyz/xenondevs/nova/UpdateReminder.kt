package xyz.xenondevs.nova

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.util.runTaskTimer
import java.net.URL

object UpdateReminder : Listener {
    
    private var needsUpdate = false
    private var taskId: Int = -1
    
    fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        taskId = runTaskTimer(0, 200) {
            println(1)
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
        val newVersion = URL("https://api.spigotmc.org/legacy/update.php?resource=93648").readText()
        val currentVersion = NOVA.description.version
        if (newVersion != currentVersion) {
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
        if (player.hasPermission("nova.updatereminder")) {
            checkVersion()
            if (needsUpdate)
                player.sendMessage("§cYou're running an outdated version of §bNova§c. " +
                    "§cPlease download the latest version at §bhttps://spigotmc.org/resources/93648")
        }
    }
    
}