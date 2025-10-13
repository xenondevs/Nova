package xyz.xenondevs.nova.context.intention

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.context.RequiredContextParamType
import xyz.xenondevs.nova.context.intention.BlockInteract.BLOCK_POS
import xyz.xenondevs.nova.context.intention.BlockInteract.BLOCK_TYPE_NOVA
import xyz.xenondevs.nova.context.intention.BlockInteract.BLOCK_TYPE_VANILLA
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.id

/**
 * Context intention for when a block is interacted with.
 */
object BlockInteract : Block<BlockInteract>() {
    
    /**
     * The block type as id.
     *
     * Autofilled by:
     * - [BLOCK_TYPE_NOVA]
     * - [BLOCK_TYPE_VANILLA]
     * - [BLOCK_POS]
     */
    override val BLOCK_TYPE: RequiredContextParamType<Key, BlockInteract> =
        HasRequiredBlock.blockType()
    
    /**
     * Whether the data of the block should be included for creative-pick block interactions.
     * Defaults to `false`.
     *
     * Autofilled by: none
     */
    val INCLUDE_DATA = DefaultingContextParamType<Boolean, BlockInteract>(
        Key(Nova, "include_data"),
        default = false
    )
    
    init {
        applyDefaults(this)
        
        addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_POS) { it.block.id })
    }
    
}