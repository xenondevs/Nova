package xyz.xenondevs.nova.world.block.behavior

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.world.block.sound.SoundGroup

fun BlockSounds(soundGroup: SoundGroup) =
    BlockSounds.Default(provider(soundGroup))

// TODO: name
interface BlockSounds {
    
    val soundGroup: SoundGroup
    
    class Default(soundGroup: Provider<SoundGroup>) : BlockSounds, BlockBehavior {
        override val soundGroup by soundGroup
    }
    
}