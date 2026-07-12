@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import org.bukkit.block.Block
import org.bukkit.block.TileState
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.intention.HasOptionalTileEntity.Companion.TILE_ENTITY_NOVA
import xyz.xenondevs.nova.context.intention.HasOptionalTileEntity.Companion.TILE_ENTITY_VANILLA
import xyz.xenondevs.nova.util.novaKey
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.world.block.tileentity.TileEntity

/**
 * A [ContextIntention] that has optional parameters about the tile entity of a block.
 *
 * ## Autofillers
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [TILE_ENTITY_NOVA] | 1. | [BLOCK] | |
 * | [TILE_ENTITY_DATA_NOVA] | 1. | [TILE_ENTITY_NOVA] | |
 */
interface HasOptionalTileEntity<I : HasOptionalTileEntity<I>> :
    HasRequiredBlock<I>,
    HasOptionalTileEntityData<I> {
    
    /**
     * The nova tile-entity of a block.
     */
    val TILE_ENTITY_NOVA: ContextParamType<TileEntity, I>
        get() = tileEntityNova()
    
    /**
     * The vanilla tile-entity of a block.
     */
    val TILE_ENTITY_VANILLA: ContextParamType<TileState, I>
        get() = tileEntityVanilla()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val TILE_ENTITY_NOVA = ContextParamType<TileEntity, Nothing>(novaKey("tile_entity_nova"))
        private val TILE_ENTITY_VANILLA = ContextParamType<TileState, Nothing>(novaKey("tile_entity_vanilla"), copy = { it.copy() as TileState })
        
        /**
         * Gets the param type for [TILE_ENTITY_NOVA].
         */
        fun <I : HasOptionalTileEntity<I>> tileEntityNova() =
            TILE_ENTITY_NOVA as ContextParamType<TileEntity, I>
        
        /**
         * Gets the param type for [TILE_ENTITY_VANILLA].
         */
        fun <I : HasOptionalTileEntity<I>> tileEntityVanilla() =
            TILE_ENTITY_VANILLA as ContextParamType<TileState, I>
        
        /**
         * Applies the default autofillers on [intention].
         */
        fun <I : HasOptionalTileEntity<I>> applyDefaults(intention: HasOptionalTileEntity<I>) = intention.apply {
            addAutofiller(TILE_ENTITY_NOVA, Autofiller.from(BLOCK, Block::novaTileEntity))
            addAutofiller(TILE_ENTITY_DATA_NOVA, Autofiller.from(TILE_ENTITY_NOVA, TileEntity::data))
            addAutofiller(TILE_ENTITY_VANILLA, Autofiller.from(BLOCK) { it.state as? TileState })
        }
        
    }
    
}
