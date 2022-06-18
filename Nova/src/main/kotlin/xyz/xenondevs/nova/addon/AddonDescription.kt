package xyz.xenondevs.nova.addon

import org.bukkit.configuration.file.YamlConfiguration
import java.io.Reader

data class AddonDescription internal constructor(
    val id: String,
    val name: String,
    val version: String,
    val main: String,
    val authors: List<String>,
    val depend: Set<String>,
    val softdepend: Set<String>,
    val spigotResourceId: Int,
    val repositories: List<String>,
    val libraries: List<String>
) : Comparable<AddonDescription> {
    
    override fun compareTo(other: AddonDescription): Int {
        val dependencies = depend + softdepend
        val otherDependencies = other.depend + other.softdepend
        
        if (dependencies.isEmpty() && otherDependencies.isEmpty()) {
            return 0 // Both depend on nothing
        }
        if (dependencies.isEmpty()) {
            return -1 // This depends on nothing, but other does
        }
        if (otherDependencies.isEmpty()) {
            return 1 // Other depends on nothing, but this does
        }
        if (dependencies.contains(other.id)) {
            return 1 // This depends on other
        }
        if (otherDependencies.contains(id)) {
            return -1 // Other depends on this
        }
        return 0 // Both depend on different things
    }
    
    internal companion object {
        
        fun deserialize(reader: Reader): AddonDescription {
            val cfg = YamlConfiguration.loadConfiguration(reader)
            
            return AddonDescription(
                cfg.getString("id")!!,
                cfg.getString("name")!!,
                cfg.getString("version")!!,
                cfg.getString("main")!!,
                cfg.getStringList("authors"),
                cfg.getStringList("depend").toHashSet(),
                cfg.getStringList("softdepend").toHashSet(),
                cfg.getInt("spigotResourceId", -1),
                cfg.getStringList("repositories"),
                cfg.getStringList("libraries")
            )
        }
        
    }
    
}