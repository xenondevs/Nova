package xyz.xenondevs.nova.util

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import java.util.*

object SoundUtils {
    
    private val soundCache = EnumMap<Material, List<Sound>>(Material::class.java)
    
    /**
     * Gets a list of sounds for this block.
     * The list is ordered as follows:
     * 1. break sound
     * 2. step sound
     * 3. place sound
     * 4. hit sound
     * 5. fall sound
     */
    fun getSoundEffects(block: Block): List<Sound> {
        if (soundCache.containsKey(block.type)) return soundCache[block.type]!!
        
        val nmsBlock = ReflectionUtils.getNMSBlock(block)
        val soundEffectType = ReflectionRegistry.NMS_BLOCK_GET_SOUND_EFFECT_TYPE_METHOD.invoke(nmsBlock, null)
        val breakSound = ReflectionRegistry.NMS_SOUND_EFFECT_TYPE_BREAK_SOUND_FIELD.get(soundEffectType)
        val stepSound = ReflectionRegistry.NMS_SOUND_EFFECT_TYPE_STEP_SOUND_FIELD.get(soundEffectType)
        val placeSound = ReflectionRegistry.NMS_SOUND_EFFECT_TYPE_PLACE_SOUND_FIELD.get(soundEffectType)
        val hitSound = ReflectionRegistry.NMS_SOUND_EFFECT_TYPE_HIT_SOUND_FIELD.get(soundEffectType)
        val fallSound = ReflectionRegistry.NMS_SOUND_EFFECT_TYPE_FALL_SOUND_FIELD.get(soundEffectType)
        
        val sounds = listOf(
            toBukkitSound(breakSound),
            toBukkitSound(stepSound),
            toBukkitSound(placeSound),
            toBukkitSound(hitSound),
            toBukkitSound(fallSound)
        )
        soundCache[block.type] = sounds
        
        return sounds
    }
    
    private fun toBukkitSound(soundEffect: Any): Sound {
        return ReflectionRegistry.CB_CRAFT_SOUND_GET_BUKKIT_METHOD.invoke(null, soundEffect) as Sound
    }
    
}