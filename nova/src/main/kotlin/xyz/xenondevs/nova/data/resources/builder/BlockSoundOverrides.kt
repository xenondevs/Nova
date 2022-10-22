package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.bukkit.Material
import org.bukkit.SoundGroup
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.util.associateWithNotNull
import xyz.xenondevs.nova.util.data.getBoolean
import xyz.xenondevs.nova.util.data.getOrNull
import xyz.xenondevs.nova.util.data.getOrPut
import xyz.xenondevs.nova.util.data.getResourceData
import xyz.xenondevs.nova.util.data.getString
import xyz.xenondevs.nova.util.data.parseJson
import xyz.xenondevs.nova.util.data.writeToFile
import xyz.xenondevs.nova.util.item.soundGroup
import java.io.File
import java.util.logging.Level

/**
 * Replaces the break, hit, step and fall sounds for blocks used by Nova to display custom blocks (note block, mushroom blocks,
 * specified armor stand hitbox blocks) with 0s .ogg files and copies the real files to the nova namespace, so they
 * can be completely controlled by the server.
 */
class BlockSoundOverrides {
    
    private val soundGroups = HashSet<SoundGroup>()
    private val soundEvents = ArrayList<String>()
    private val empty = getResourceData("empty.ogg")
    
    fun useMaterial(material: Material) {
        val soundGroup = material.soundGroup
        if (soundGroup !in soundGroups) {
            addSoundGroup(soundGroup)
            soundGroups += soundGroup
        }
    }
    
    private fun addSoundGroup(group: SoundGroup) {
        soundEvents += group.breakSound.key.key
        soundEvents += group.hitSound.key.key
        soundEvents += group.stepSound.key.key
        soundEvents += group.fallSound.key.key
    }
    
    fun write() {
        try {
            // and index of all vanilla sounds
            val vanillaIndex = createSoundsIndex(
                File(ResourcePackBuilder.MCASSETS_ASSETS_DIR, "minecraft/sounds.json")
                    .parseJson() as JsonObject
            )
            
            // merge the sound.json files
            val merged = mergeSoundJsons(
                File(ResourcePackBuilder.MCASSETS_ASSETS_DIR, "minecraft/sounds.json"),
                File(ResourcePackBuilder.ASSETS_DIR, "minecraft/sounds.json")
            )
            
            // an index of all sounds (vanilla and base packs)
            val index = createSoundsIndex(merged)
            
            // override all required .ogg files
            val soundFilePaths = soundEvents.flatMapTo(HashSet()) { getSoundPaths(index, it) }
            val soundPathMappings = soundFilePaths.associateWithNotNull(::overrideSoundFile)
            
            // create and write Nova's sounds.json
            val novaSoundIndex = JsonObject()
            soundEvents.forEach { soundEvent ->
                val soundEventObj = index[soundEvent]!!
                replaceSoundPaths(soundEventObj, soundPathMappings)
                novaSoundIndex.add(soundEvent, soundEventObj)
            }
            novaSoundIndex.writeToFile(File(ResourcePackBuilder.ASSETS_DIR, "nova/sounds.json"))

            // replace the sound paths in minecraft/sounds.json for all sound events that aren't supposed to be replaced
            // (for example the sound event block.stone.place uses the same sounds as block.stone.break, but we don't actually
            // want to disable the clientside placing sounds)
            val mcSoundIndex = JsonObject()
            index.forEach { (soundEvent, soundEventObj) ->
                if (soundEvent in soundEvents)
                    return@forEach
                
                if (replaceSoundPaths(soundEventObj, soundPathMappings)) {
                    // replace sounds of packs below
                    soundEventObj.addProperty("replace", true)
                    // add to minecraft/sounds.json
                    mcSoundIndex.add(soundEvent, soundEventObj)
                } else if (soundEvent !in vanillaIndex) {
                    // needs to be added to minecraft/sounds.json too as it is not a vanilla sound
                    mcSoundIndex.add(soundEvent, soundEventObj)
                }
            }
            mcSoundIndex.writeToFile(File(ResourcePackBuilder.ASSETS_DIR, "minecraft/sounds.json"))
            
            // write overridden sound events to permanent storage
            PermanentStorage.store("soundOverrides", soundEvents)
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to write block sound overrides", e)
        }
    }
    
    private fun createSoundsIndex(obj: JsonObject): Map<String, JsonObject> {
        return obj.entrySet().associateTo(HashMap()) { it.key to it.value as JsonObject }
    }
    
    private fun mergeSoundJsons(vararg files: File): JsonObject {
        val jsonObjects = files.mapNotNull { it.takeIf(File::exists)?.parseJson() as? JsonObject }
        return if (jsonObjects.size > 1) {
            mergeSoundJsons(jsonObjects)
        } else jsonObjects[0]
    }
    
    private fun mergeSoundJsons(soundJsons: List<JsonObject>): JsonObject {
        require(soundJsons.size > 1)
        
        val merged = soundJsons[0].deepCopy()
        
        val otherJsons = soundJsons.subList(0, soundJsons.size)
        otherJsons.forEach { mainObj ->
            mainObj.entrySet().forEach { (soundEvent, soundEventObj) ->
                soundEventObj as JsonObject
                
                val mergedSoundEventObj = merged.getOrPut(soundEvent, ::JsonObject)
                
                val subtitle = soundEventObj.getString("subtitle")
                val replace = soundEventObj.getBoolean("replace")
                
                // write non-sound entries
                if (subtitle != null)
                    mergedSoundEventObj.addProperty("subtitle", subtitle)
                if (replace)
                    mergedSoundEventObj.addProperty("replace", true)
                
                // merge sounds
                val destSounds = if (replace)
                    JsonArray().also { mergedSoundEventObj.add("sounds", it) }
                else mergedSoundEventObj.getOrPut("sounds", ::JsonArray)
                
                soundEventObj.getAsJsonArray("sounds").forEach(destSounds::add)
            }
            
        }
        
        return merged
    }
    
    private fun getSoundPaths(index: Map<String, JsonObject>, sound: String): Set<ResourcePath> {
        val obj = index[sound] ?: return emptySet()
        val sounds = obj.getOrNull("sounds") as JsonArray
        return sounds.mapTo(HashSet()) {
            ResourcePath.of(
                when (it) {
                    is JsonPrimitive -> it.asString
                    is JsonObject -> it.getString("name")!!
                    else -> throw UnsupportedOperationException()
                },
                "minecraft"
            )
        }
    }
    
    private fun overrideSoundFile(path: ResourcePath): ResourcePath? {
        // destination for the empty.ogg file
        val emptyDest = File(ResourcePackBuilder.ASSETS_DIR, "${path.namespace}/sounds/${path.path}.ogg")
        
        // the source sound might be at the custom resource pack location (because of base packs) or in the mc assets
        val source = emptyDest.takeIf(File::exists)
            ?: File(ResourcePackBuilder.MCASSETS_ASSETS_DIR, "${path.namespace}/sounds/${path.path}.ogg")
        
        // check for missing source file
        if (!source.exists()) {
            LOGGER.warning("Sound file does not exist: $path")
            return null
        }
        
        // destination for the real sound file
        val destPath = "overrides/${path.namespace}/${path.path}"
        val dest = File(ResourcePackBuilder.ASSETS_DIR, "nova/sounds/$destPath.ogg")
        
        // create dirs
        emptyDest.parentFile.mkdirs()
        dest.parentFile.mkdirs()
        
        // write files
        source.copyTo(dest)
        emptyDest.writeBytes(empty)
        
        return ResourcePath("nova", destPath)
    }
    
    private fun replaceSoundPaths(soundEvent: JsonObject, mappings: Map<ResourcePath, ResourcePath>): Boolean {
        val sourceSounds = soundEvent.getAsJsonArray("sounds")
        val destSounds = JsonArray()
    
        var changed = false
        
        fun resolveMapping(path: String): String {
            val rPath = ResourcePath.of(path, "minecraft")
            return if (rPath in mappings) {
                changed = true
                mappings[rPath]!!.toString()
            } else path
        }
        
        sourceSounds.forEach { sound ->
            when (sound) {
                is JsonPrimitive -> destSounds.add(resolveMapping(sound.asString))
                is JsonObject -> {
                    sound.addProperty("name", resolveMapping(sound.getString("name")!!))
                    destSounds.add(sound)
                }
                
                else -> throw UnsupportedOperationException()
            }
        }
        
        soundEvent.add("sounds", destSounds)
        
        return changed
    }
    
}