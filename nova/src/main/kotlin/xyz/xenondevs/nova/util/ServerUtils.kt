package xyz.xenondevs.nova.util

import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_20_R3.CraftServer
import xyz.xenondevs.nova.util.ServerSoftware.*

object ServerUtils {
    
    val SERVER_SOFTWARE by lazy {
        when (Bukkit.getVersion().substringAfter('-').substringBefore('-')) {
            "Bukkit" -> CRAFT_BUKKIT
            "Spigot" -> SPIGOT
            "Paper" -> PAPER
            "Folia" -> FOLIA
            "Pufferfish" -> PUFFERFISH
            "Purpur" -> PURPUR
            else -> UNKNOWN
        }
    }
    
    fun isReload(): Boolean = (Bukkit.getServer() as CraftServer).reloadCount != 0
    
}

enum class ServerSoftware(private val upstream: ServerSoftware? = null) {
    
    UNKNOWN,
    CRAFT_BUKKIT,
    SPIGOT(CRAFT_BUKKIT),
    PAPER(SPIGOT),
    FOLIA(PAPER),
    PUFFERFISH(PAPER),
    PURPUR(PUFFERFISH);
    
    val superSoftwares: List<ServerSoftware>
    
    init {
        val superSoftwares = ArrayList<ServerSoftware>()
        var software: ServerSoftware? = this
        while (software != null) {
            superSoftwares.add(software)
            software = software.upstream
        }
        this.superSoftwares = superSoftwares
    }
    
}