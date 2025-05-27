package xyz.xenondevs.nova.world.entity.variant

import net.kyori.adventure.key.Key
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant
import org.bukkit.craftbukkit.entity.CraftWolf
import org.bukkit.entity.Wolf
import xyz.xenondevs.nova.registry.LazyRegistryElementBuilder
import xyz.xenondevs.nova.util.toResourceLocation
import java.util.*

class WolfSoundVariantBuilder(id: Key) : LazyRegistryElementBuilder<Wolf.SoundVariant, WolfSoundVariant>(
    Registries.WOLF_SOUND_VARIANT,
    CraftWolf.CraftSoundVariant::minecraftHolderToBukkit,
    id
) {
    
    private var ambientSound: Holder<SoundEvent>? = null
    private var deathSound: Holder<SoundEvent>? = null
    private var growlSound: Holder<SoundEvent>? = null
    private var hurtSound: Holder<SoundEvent>? = null
    private var pantSound: Holder<SoundEvent>? = null
    private var whineSound: Holder<SoundEvent>? = null
    
    /**
     * Configures the sound that is played regularly.
     */
    fun ambientSound(ambientSound: Key, fixedRange: Float? = null) {
        this.ambientSound = Holder.direct(SoundEvent(ambientSound.toResourceLocation(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound that is played when the wolf dies.
     */
    fun deathSound(deathSound: Key, fixedRange: Float? = null) {
        this.deathSound = Holder.direct(SoundEvent(deathSound.toResourceLocation(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound that is played when the wolf growls.
     */
    fun growlSound(growlSound: Key, fixedRange: Float? = null) {
        this.growlSound = Holder.direct(SoundEvent(growlSound.toResourceLocation(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound that is played when the wolf gets hurt.
     */
    fun hurtSound(hurtSound: Key, fixedRange: Float? = null) {
        this.hurtSound = Holder.direct(SoundEvent(hurtSound.toResourceLocation(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound that is played when the wolf pants.
     */
    fun pantSound(pantSound: Key, fixedRange: Float? = null) {
        this.pantSound = Holder.direct(SoundEvent(pantSound.toResourceLocation(), Optional.ofNullable(fixedRange)))
    }
    
    /**
     * Configures the sound that is played when the wolf whines.
     */
    fun whineSound(whineSound: Key, fixedRange: Float? = null) {
        this.whineSound = Holder.direct(SoundEvent(whineSound.toResourceLocation(), Optional.ofNullable(fixedRange)))
    }
    
    override fun build(lookup: RegistryOps.RegistryInfoLookup): WolfSoundVariant {
        val undefinedSound = Holder.direct(SoundEvent(ResourceLocation.parse("nova:undefined"), Optional.empty()))
        return WolfSoundVariant(
            ambientSound ?: undefinedSound,
            deathSound ?: undefinedSound,
            growlSound ?: undefinedSound,
            hurtSound ?: undefinedSound,
            pantSound ?: undefinedSound,
            whineSound ?: undefinedSound
        )
    }
    
}