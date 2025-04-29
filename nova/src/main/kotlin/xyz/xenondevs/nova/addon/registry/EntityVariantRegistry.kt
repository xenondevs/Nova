package xyz.xenondevs.nova.addon.registry

import org.bukkit.entity.Cat
import org.bukkit.entity.Chicken
import org.bukkit.entity.Cow
import org.bukkit.entity.Frog
import org.bukkit.entity.Pig
import org.bukkit.entity.Wolf
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.world.entity.CatVariantBuilder
import xyz.xenondevs.nova.world.entity.ChickenVariantBuilder
import xyz.xenondevs.nova.world.entity.CowVariantBuilder
import xyz.xenondevs.nova.world.entity.FrogVariantBuilder
import xyz.xenondevs.nova.world.entity.PigVariantBuilder
import xyz.xenondevs.nova.world.entity.WolfSoundVariantBuilder
import xyz.xenondevs.nova.world.entity.WolfVariantBuilder

interface EntityVariantRegistry : AddonGetter {
    
    /**
     * Registers a new [Cat.Type] under [name] after configuring it with [catVariant].
     */
    fun catVariant(name: String, catVariant: CatVariantBuilder.() -> Unit): Provider<Cat.Type> =
        CatVariantBuilder(Key(addon, name)).apply(catVariant).register()
    
    /**
     * Registers a new [Chicken.Variant] under [name] after configuring it with [chickenVariant].
     */
    fun chickenVariant(name: String, chickenVariant: ChickenVariantBuilder.() -> Unit): Provider<Chicken.Variant> =
        ChickenVariantBuilder(Key(addon, name)).apply(chickenVariant).register()
    
    /**
     * Registers a new [Cow.Variant] under [name] after configuring it with [cowVariant].
     */
    fun cowVariant(name: String, cowVariant: CowVariantBuilder.() -> Unit): Provider<Cow.Variant> =
        CowVariantBuilder(Key(addon, name)).apply(cowVariant).register()
    
    /**
     * Registers a new [Frog.Variant] under [name] after configuring it with [cowVariant].
     */
    fun frogVariant(name: String, cowVariant: FrogVariantBuilder.() -> Unit): Provider<Frog.Variant> =
        FrogVariantBuilder(Key(addon, name)).apply(cowVariant).register()
    
    /**
     * Registers a new [Pig.Variant] under [name] after configuring it with [cowVariant].
     */
    fun pigVariant(name: String, cowVariant: PigVariantBuilder.() -> Unit): Provider<Pig.Variant> =
        PigVariantBuilder(Key(addon, name)).apply(cowVariant).register()
    
    /**
     * Registers a new [Wolf.Variant] under [name] after configuring it with [wolfVariant].
     */
    fun wolfVariant(name: String, wolfVariant: WolfVariantBuilder.() -> Unit): Provider<Wolf.Variant> =
        WolfVariantBuilder(Key(addon, name)).apply(wolfVariant).register()
    
    /**
     * Registers a new [Wolf.SoundVariant] under [name] after configuring it with [wolfSoundVariant].
     */
    fun wolfSoundVariant(name: String, wolfSoundVariant: WolfSoundVariantBuilder.() -> Unit): Provider<Wolf.SoundVariant> =
        WolfSoundVariantBuilder(Key(addon, name)).apply(wolfSoundVariant).register()
    
}