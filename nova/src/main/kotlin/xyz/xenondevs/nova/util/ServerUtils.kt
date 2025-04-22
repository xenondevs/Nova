package xyz.xenondevs.nova.util

import io.papermc.paper.ServerBuildInfo
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftServer
import xyz.xenondevs.commons.version.Version
import xyz.xenondevs.nova.util.ServerSoftware.*

/**
 * The game version of the server.
 */
val SERVER_VERSION: Version = run {
    val versionPattern = Regex("""(1\.\d{1,2}(\.\d{1,2})?)""")
    Version(versionPattern.find(ServerBuildInfo.buildInfo().minecraftVersionId())!!.groupValues[1])
}

object ServerUtils {
    
    val SERVER_SOFTWARE by lazy {
        when (ServerBuildInfo.buildInfo().brandId()) {
            Key.key("papermc", "paper") -> PAPER
            Key.key("papermc", "folia") -> FOLIA
            Key.key("pufferfish", "pufferfish") -> PUFFERFISH
            Key.key("purpurmc", "purpur") -> PURPUR
            else -> UNKNOWN
        }
    }
    
    fun isReload(): Boolean = (Bukkit.getServer() as CraftServer).reloadCount != 0
    
}

enum class ServerSoftware(private val upstream: ServerSoftware? = null) {
    
    UNKNOWN,
    PAPER(),
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