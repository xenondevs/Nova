package xyz.xenondevs.nova.addon

import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.nova.serialization.json.getDeserialized
import xyz.xenondevs.nova.util.data.Version

data class AddonDescription internal constructor(
    val id: String,
    val name: String,
    val version: String,
    val main: String,
    val novaVersion: Version,
    val authors: List<String>,
    val depend: Set<String>,
    val softdepend: Set<String>
) {
    
    internal companion object {
        
        fun fromJson(obj: JsonObject) = AddonDescription(
            obj.getString("id"),
            obj.getString("name"),
            obj.getString("version"),
            obj.getString("main"),
            obj.getDeserialized("nova_version"),
            obj.getDeserialized("authors"),
            obj.getDeserialized<HashSet<String>>("depend"),
            obj.getDeserialized<HashSet<String>>("softdepend")
        )
        
    }
    
}