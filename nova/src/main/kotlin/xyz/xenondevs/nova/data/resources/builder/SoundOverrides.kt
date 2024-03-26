package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import org.bukkit.Material
import org.bukkit.SoundGroup
import xyz.xenondevs.commons.gson.getBooleanOrNull
import xyz.xenondevs.commons.gson.getOrPut
import xyz.xenondevs.commons.gson.getStringOrNull
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.task.PackTask
import xyz.xenondevs.nova.data.resources.builder.task.PackTaskHolder
import xyz.xenondevs.nova.util.item.soundGroup
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.exists

/**
 * Removes the break, hit, step and fall sounds for blocks used by Nova to display custom blocks (note block, mushroom blocks,
 * specified armor stand hitbox blocks) and all armor equip sounds and copies them to the Nova namespace, so that they can be completely controlled by the server.
 */
class SoundOverrides internal constructor(builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val soundGroups = HashSet<SoundGroup>()
    private val soundEvents = ArrayList<String>()
    
    init {
        // Armor equip sounds
        addSoundEvent(SoundEvents.ARMOR_EQUIP_CHAIN)
        addSoundEvent(SoundEvents.ARMOR_EQUIP_DIAMOND)
        addSoundEvent(SoundEvents.ARMOR_EQUIP_ELYTRA)
        addSoundEvent(SoundEvents.ARMOR_EQUIP_GENERIC)
        addSoundEvent(SoundEvents.ARMOR_EQUIP_GOLD)
        addSoundEvent(SoundEvents.ARMOR_EQUIP_IRON)
        addSoundEvent(SoundEvents.ARMOR_EQUIP_LEATHER)
        addSoundEvent(SoundEvents.ARMOR_EQUIP_NETHERITE)
        addSoundEvent(SoundEvents.ARMOR_EQUIP_TURTLE)
    }
    
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
    
    private fun addSoundEvent(event: SoundEvent) {
        soundEvents += event.location.path
    }
    
    @PackTask
    private fun write() {
        try {
            // an index of all vanilla sounds
            val vanillaIndex = createSoundsIndex(
                ResourcePackBuilder.MCASSETS_ASSETS_DIR.resolve("minecraft/sounds.json").parseJson() as JsonObject
            )
            
            // merge the sound.json files
            val merged = mergeSoundJsons(
                ResourcePackBuilder.MCASSETS_ASSETS_DIR.resolve("minecraft/sounds.json"),
                ResourcePackBuilder.ASSETS_DIR.resolve("minecraft/sounds.json")
            )
            
            // an index of all sounds (vanilla and base packs)
            val index = createSoundsIndex(merged)
            
            // create and write Nova's sounds.json
            val novaSoundIndex = JsonObject()
            soundEvents.forEach { soundEvent ->
                val soundEventObj = index[soundEvent]!!
                novaSoundIndex.add(soundEvent, soundEventObj)
            }
            novaSoundIndex.writeToFile(ResourcePackBuilder.ASSETS_DIR.resolve("nova/sounds.json"))
            
            val mcSoundIndex = JsonObject()
            index.forEach { (soundEvent, soundEventObj) ->
                if (soundEvent in soundEvents) {
                    // replace sounds of lower packs
                    soundEventObj.addProperty("replace", true)
                    // disable subtitles as there is no actual sound
                    soundEventObj.remove("subtitle")
                    // set empty sound array
                    soundEventObj.add("sounds", JsonArray())
                    // add to minecraft/sounds.json
                    mcSoundIndex.add(soundEvent, soundEventObj)
                    
                    return@forEach
                }
                
                if (soundEvent !in vanillaIndex) {
                    // needs to be added to minecraft/sounds.json too as it is not a vanilla sound
                    mcSoundIndex.add(soundEvent, soundEventObj)
                }
            }
            mcSoundIndex.writeToFile(ResourcePackBuilder.ASSETS_DIR.resolve("minecraft/sounds.json"))
            
            // write overridden sound events to permanent storage
            PermanentStorage.store("soundOverrides", soundEvents)
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to write block sound overrides", e)
        }
    }
    
    private fun createSoundsIndex(obj: JsonObject): Map<String, JsonObject> {
        return obj.entrySet().associateTo(HashMap()) { it.key to it.value as JsonObject }
    }
    
    private fun mergeSoundJsons(vararg files: Path): JsonObject {
        val jsonObjects = files.mapNotNull { it.takeIf(Path::exists)?.parseJson() as? JsonObject }
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
                
                val subtitle = soundEventObj.getStringOrNull("subtitle")
                val replace = soundEventObj.getBooleanOrNull("replace") ?: false
                
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
    
}