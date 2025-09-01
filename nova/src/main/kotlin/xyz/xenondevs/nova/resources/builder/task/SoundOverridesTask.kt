package xyz.xenondevs.nova.resources.builder.task

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.world.level.block.SoundType
import org.bukkit.Material
import xyz.xenondevs.commons.gson.getBooleanOrNull
import xyz.xenondevs.commons.gson.getOrPut
import xyz.xenondevs.commons.gson.getStringOrNull
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.nmsBlock
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Removes the break, hit, step and fall sounds for blocks used by Nova to display custom blocks (note block, mushroom blocks,
 * specified armor stand hitbox blocks) and copies them to the Nova namespace, so that they can be completely controlled by the server.
 */
class SoundOverridesTask(private val builder: ResourcePackBuilder) : PackTask {
    
    override val stage = BuildStage.PRE_WORLD
    override val runsAfter = setOf(BlockModelTask::class)
    
    private val soundEvents = HashSet<String>()
    
    private fun useMaterial(material: Material) {
        val soundType = material.nmsBlock.defaultBlockState().soundType
        addSoundType(soundType)
    }
    
    private fun addSoundType(type: SoundType) {
        soundEvents += type.breakSound.location.path
        soundEvents += type.hitSound.location.path
        soundEvents += type.stepSound.location.path
        soundEvents += type.fallSound.location.path
    }
    
    override suspend fun run() {
        findUsedBlockTypes()
        write()
    }
    
    private fun findUsedBlockTypes() {
        for (block in NovaRegistries.BLOCK) {
            for (blockState in block.blockStates) {
                when (val modelProvider = blockState.modelProvider) {
                    is DisplayEntityBlockModelProvider -> useMaterial(modelProvider.info.hitboxType.bukkitMaterial)
                    is BackingStateBlockModelProvider -> useMaterial(modelProvider.info.vanillaBlockState.bukkitMaterial)
                    is ModelLessBlockModelProvider -> useMaterial(modelProvider.info.bukkitMaterial)
                }
            }
        }
    }
    
    private fun write() {
        try {
            // an index of all vanilla sounds
            val vanillaIndex = createSoundsIndex(
                builder.resolveVanilla("assets/minecraft/sounds.json").parseJson() as JsonObject
            )
            
            // merge the sound.json files
            val merged = mergeSoundJsons(
                builder.resolveVanilla("assets/minecraft/sounds.json"),
                builder.resolve("assets/minecraft/sounds.json"),
            )
            
            // an index of all sounds (vanilla and base packs)
            val index = createSoundsIndex(merged)
            
            // create and write Nova's sounds.json
            val novaSoundIndex = JsonObject()
            soundEvents.forEach { soundEvent ->
                val soundEventObj = index[soundEvent]!!
                novaSoundIndex.add(soundEvent, soundEventObj)
            }
            novaSoundIndex.writeToFile(builder.resolve("assets/nova/sounds.json"))
            
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
            mcSoundIndex.writeToFile(builder.resolve("assets/minecraft/sounds.json"))
            
            // write overridden sound events to permanent storage
            ResourceLookups.SOUND_OVERRIDES = soundEvents
        } catch (e: Exception) {
            builder.logger.error("Failed to write block sound overrides", e)
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