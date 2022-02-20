package xyz.xenondevs.nova.addon

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import xyz.xenondevs.nova.util.data.getAllStrings
import xyz.xenondevs.nova.util.data.getAllStringsTo
import xyz.xenondevs.nova.util.data.getInt
import xyz.xenondevs.nova.util.data.getString

data class AddonDescription(
    val id: String,
    val name: String,
    val version: String,
    val main: String,
    val authors: List<String>,
    val depend: Set<String>,
    val softdepend: Set<String>,
    val spigotResourceId: Int
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
    
    companion object {
        
        fun deserialize(element: JsonElement): AddonDescription {
            element as JsonObject
            
            return AddonDescription(
                element.getString("id")!!,
                element.getString("name")!!,
                element.getString("version")!!,
                element.getString("main")!!,
                element.getAsJsonArray("authors")?.getAllStrings() ?: emptyList(),
                element.getAsJsonArray("depend")?.getAllStringsTo(HashSet()) ?: emptySet(),
                element.getAsJsonArray("softdepend")?.getAllStringsTo(HashSet()) ?: emptySet(),
                element.getInt("spigotResourceId") ?: -1
            )
        }
        
    }
    
}