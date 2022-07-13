package xyz.xenondevs.nova.util

import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.ServerSoftware.*
import java.util.concurrent.ConcurrentHashMap

object ServerUtils {
    
    val SERVER_SOFTWARE by lazy {
        when (Bukkit.getVersion().substringAfter('-').substringBefore('-')) {
            "Bukkit" -> CRAFT_BUKKIT
            "Spigot" -> SPIGOT
            "Paper" -> PAPER
            "Tuinity" -> TUINITY
            "Purpur" -> PURPUR
            "Airplane" -> AIRPLANE
            else -> UNKNOWN
        }
    }
    
    val isReload = (NOVA.server as CraftServer).reloadCount != 0
    
}


enum class ServerSoftware {
    CRAFT_BUKKIT,
    SPIGOT,
    PAPER,
    TUINITY,
    PURPUR,
    AIRPLANE,
    UNKNOWN;
    fun <K, V> getCorrectMap(): MutableMap<K, V> = if (this == PURPUR) ConcurrentHashMap() else HashMap()
    
}
