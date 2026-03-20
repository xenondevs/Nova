package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import net.minecraft.core.Holder
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant
import org.bukkit.entity.Wolf
import xyz.xenondevs.nova.util.toIdentifier
import java.util.*

internal class WolfSoundVariantBuilderImpl(
    override val entry: RegistryEntry.Paper<Wolf.SoundVariant>
) : RegistryElementBuilder.Vanilla<WolfSoundVariant>, WolfSoundVariantBuilder {
    
    private var configureAdultSounds: (WolfSoundSetBuilder.() -> Unit)? = null
    private var configureBabySounds: (WolfSoundSetBuilder.() -> Unit)? = null
    
    /**
     * Configures the adult wolf sound set.
     */
    override fun adultSounds(adultSounds: WolfSoundSetBuilder.() -> Unit) {
        this.configureAdultSounds = adultSounds
    }
    
    /**
     * Configures the baby wolf sound set.
     *
     * If not specified, the adult sounds are used.
     */
    override fun babySounds(babySounds: WolfSoundSetBuilder.() -> Unit) {
        this.configureBabySounds = babySounds
    }
    
    override fun build(lookup: RegistryOps.RegistryInfoLookup): WolfSoundVariant {
        val undefinedSound = Holder.direct(SoundEvent(Identifier.parse("nova:undefined"), Optional.empty()))
        val adultSet = WolfSoundSetBuilder()
            .apply(configureAdultSounds ?: throw IllegalStateException("Adult sounds are not defined"))
            .build(undefinedSound)
        val babySet = configureBabySounds
            ?.let { WolfSoundSetBuilder().apply(it).build(undefinedSound) }
            ?: adultSet
        return WolfSoundVariant(adultSet, babySet)
    }
    
}

@RegistryElementBuilderDsl
class WolfSoundSetBuilder internal constructor() {
    
    private var ambientSound: Holder<SoundEvent>? = null
    private var deathSound: Holder<SoundEvent>? = null
    private var growlSound: Holder<SoundEvent>? = null
    private var hurtSound: Holder<SoundEvent>? = null
    private var pantSound: Holder<SoundEvent>? = null
    private var whineSound: Holder<SoundEvent>? = null
    private var stepSound: Holder<SoundEvent>? = null
    
    /**
     * Configures the sound played regularly.
     */
    fun ambientSound(ambientSound: Key, fixedRange: Float? = null) {
        this.ambientSound = Holder.direct(SoundEvent(ambientSound.toIdentifier(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound played when the wolf dies.
     */
    fun deathSound(deathSound: Key, fixedRange: Float? = null) {
        this.deathSound = Holder.direct(SoundEvent(deathSound.toIdentifier(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound played when the wolf growls.
     */
    fun growlSound(growlSound: Key, fixedRange: Float? = null) {
        this.growlSound = Holder.direct(SoundEvent(growlSound.toIdentifier(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound played when the wolf gets hurt.
     */
    fun hurtSound(hurtSound: Key, fixedRange: Float? = null) {
        this.hurtSound = Holder.direct(SoundEvent(hurtSound.toIdentifier(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound played when the wolf pants.
     */
    fun pantSound(pantSound: Key, fixedRange: Float? = null) {
        this.pantSound = Holder.direct(SoundEvent(pantSound.toIdentifier(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound played when the wolf whines.
     */
    fun whineSound(whineSound: Key, fixedRange: Float? = null) {
        this.whineSound = Holder.direct(SoundEvent(whineSound.toIdentifier(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound played when the wolf steps.
     */
    fun stepSound(stepSound: Key, fixedRange: Float? = null) {
        this.stepSound = Holder.direct(SoundEvent(stepSound.toIdentifier(), Optional.ofNullable(fixedRange)))
    }
    
    internal fun build(undefinedSound: Holder<SoundEvent>) = WolfSoundVariant.WolfSoundSet(
        ambientSound ?: undefinedSound,
        deathSound ?: undefinedSound,
        growlSound ?: undefinedSound,
        hurtSound ?: undefinedSound,
        pantSound ?: undefinedSound,
        whineSound ?: undefinedSound,
        stepSound ?: undefinedSound,
    )
    
}
