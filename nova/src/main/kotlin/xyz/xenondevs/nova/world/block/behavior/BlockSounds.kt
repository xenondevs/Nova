package xyz.xenondevs.nova.world.block.behavior

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.world.block.sound.SoundGroup

/**
 * Adds sound to a block.
 * 
 * @param soundGroup The sound group to be used.
 */
class BlockSounds(soundGroup: Provider<SoundGroup>) : BlockBehavior {
    
    /**
     * The sound group.
     */
    val soundGroup: SoundGroup by soundGroup
    
    /**
     * Adds sound to a block.
     * 
     * @param soundGroup The sound group to be used.
     */
    constructor(soundGroup: SoundGroup) : this(provider(soundGroup))
    
}