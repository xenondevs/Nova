package xyz.xenondevs.nova.tileentity.impl.processing.brewing

import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.data.recipe.MechanicalBrewingStandRecipe
import kotlin.math.pow
import kotlin.math.roundToInt

// TODO: clean up
class PotionEffectBuilder(
    type: PotionEffectType? = null,
    durationLevel: Int = 0,
    amplifierLevel: Int = 0
) : Cloneable {
    
    val recipe: MechanicalBrewingStandRecipe
        get() = MechanicalBrewingStand.AVAILABLE_POTION_EFFECTS[type]!!
    
    var type: PotionEffectType? = type
        set(value) {
            field = value
            if (value != null) {
                val recipe = recipe
                maxDurationLevel = recipe.maxDurationLevel
                maxAmplifierLevel = recipe.maxAmplifierLevel
            } else {
                maxDurationLevel = 0
                maxAmplifierLevel = 0
            }
        }
    
    var maxDurationLevel: Int = 0
        private set
    var maxAmplifierLevel: Int = 0
        private set
    
    var durationLevel: Int = durationLevel
        set(value) {
            field = value.coerceIn(0..maxDurationLevel)
        }
    
    var amplifierLevel: Int = amplifierLevel
        set(value) {
            field = value.coerceIn(0..maxAmplifierLevel)
        }
    
    init {
        if (type != null) {
            val recipe = recipe
            maxDurationLevel = recipe.maxDurationLevel
            maxAmplifierLevel = recipe.maxAmplifierLevel
        }
    }
    
    fun build(): PotionEffect {
        val type = type
        requireNotNull(type)
        
        val recipe = recipe
        val defaultDuration = recipe.defaultTime
        var duration = defaultDuration.toDouble()
        if (durationLevel > 0) duration *= durationLevel * recipe.redstoneMultiplier
        duration *= recipe.glowstoneMultiplier.pow(amplifierLevel)
        
        return PotionEffect(type, duration.roundToInt(), amplifierLevel, false, true, true)
    }
    
    public override fun clone(): PotionEffectBuilder {
        return super.clone() as PotionEffectBuilder
    }
    
    companion object {
        
        fun of(effect: PotionEffect) = PotionEffectBuilder(effect.type, effect.duration, effect.amplifier)
        
    }
    
}