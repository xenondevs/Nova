@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.format.WorldDataManager

/**
 * A [ContextIntention] that has optional parameters about the tile entity of a block.
 */
interface HasOptionalTileEntity<I : HasOptionalTileEntity<I>> : HasRequiredBlock<I> {
    
    /**
     * The nova tile-entity of a block.
     *
     * Autofilled by:
     * - [BLOCK_POS]
     */
    val TILE_ENTITY_NOVA: ContextParamType<TileEntity, I>
    
    /**
     * The tile-entity data of a nova tile-entity.
     *
     * Autofilled by:
     * - [TILE_ENTITY_NOVA]
     */
    val TILE_ENTITY_DATA_NOVA: ContextParamType<Compound, I>
    
    companion object {
        
        /**
         * Creates a param type for [TILE_ENTITY_NOVA].
         */
        fun <I : ContextIntention<I>> tileEntityNova() =
            ContextParamType<TileEntity, I>(Key(Nova, "tile_entity_nova"))
        
        /**
         * Creates a param type for [TILE_ENTITY_DATA_NOVA].
         */
        fun <I : ContextIntention<I>> tileEntityDataNova() =
            ContextParamType<Compound, I>(Key(Nova, "tile_entity_data_nova"), copy = Compound::copy)
        
        /**
         * Applies the default autofillers on [intention].
         */
        fun <I : HasOptionalTileEntity<I>> applyDefaults(intention: HasOptionalTileEntity<I>) = intention.apply {
            addAutofiller(TILE_ENTITY_NOVA, Autofiller.from(BLOCK_POS, WorldDataManager::getTileEntity))
            addAutofiller(TILE_ENTITY_DATA_NOVA, Autofiller.from(TILE_ENTITY_NOVA, TileEntity::data))
        }
        
    }
    
}