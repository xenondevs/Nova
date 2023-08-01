package xyz.xenondevs.nova.util

import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_20_R1.CraftServer
import xyz.xenondevs.nova.util.ServerSoftware.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ServerUtils {
    
    val SERVER_SOFTWARE by lazy {
        when (Bukkit.getVersion().substringAfter('-').substringBefore('-')) {
            "Bukkit" -> CRAFT_BUKKIT
            "Spigot" -> SPIGOT
            "Paper" -> PAPER
            "Pufferfish" -> PUFFERFISH
            "Tuinity" -> TUINITY
            "Purpur" -> PURPUR
            "Airplane" -> AIRPLANE
            else -> UNKNOWN
        }
    }
    
    fun isReload(): Boolean = (Bukkit.getServer() as CraftServer).reloadCount != 0
    
}

enum class ServerSoftware(private vararg val superSoftwares: ServerSoftware = emptyArray()) {
    
    CRAFT_BUKKIT,
    SPIGOT(CRAFT_BUKKIT),
    PAPER(SPIGOT),
    PUFFERFISH(PAPER),
    TUINITY(PAPER),
    PURPUR(TUINITY, PUFFERFISH),
    AIRPLANE(PURPUR),
    UNKNOWN;
    
    val tree: List<ServerSoftware> = buildList { 
        val unexplored = LinkedList<ServerSoftware>()
        unexplored += this@ServerSoftware
        
        generateSequence { unexplored.poll() }
            .forEach { software -> 
                add(software)
                unexplored += software.superSoftwares
            }
    }
    
    fun <K, V> getCorrectMap(): MutableMap<K, V> = if (this == PURPUR) ConcurrentHashMap() else HashMap()
    
    fun isPaper() = this.tree.contains(PAPER)
    
}