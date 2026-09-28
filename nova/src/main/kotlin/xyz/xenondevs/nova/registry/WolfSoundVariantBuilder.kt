package xyz.xenondevs.nova.registry

import org.bukkit.entity.Wolf

/**
 * A builder for [Wolf.SoundVariant].
 */
@RegistryElementBuilderDsl
sealed interface WolfSoundVariantBuilder : RegistryEntryBuilder.Paper<Wolf.SoundVariant> {
    
    /**
     * Configures the adult wolf sound set.
     */
    fun adultSounds(adultSounds: WolfSoundSetBuilder.() -> Unit)
    
    /**
     * Configures the baby wolf sound set.
     *
     * If not specified, the adult sounds are used.
     */
    fun babySounds(babySounds: WolfSoundSetBuilder.() -> Unit)
    
}
