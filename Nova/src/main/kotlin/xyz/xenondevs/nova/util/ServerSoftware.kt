package xyz.xenondevs.nova.util

import org.bukkit.Bukkit
import xyz.xenondevs.nova.util.ServerSoftware.*
import java.util.concurrent.ConcurrentHashMap

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