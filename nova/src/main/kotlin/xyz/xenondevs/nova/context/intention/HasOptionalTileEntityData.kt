@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.intention.HasOptionalTileEntityData.Companion.TILE_ENTITY_DATA_NOVA
import xyz.xenondevs.nova.util.novaKey

/**
 * A [ContextIntention] that optionally carries the data of a tile entity.
 */
interface HasOptionalTileEntityData<I : HasOptionalTileEntityData<I>> : ContextIntention<I> {
    
    /**
     * The data of a Nova tile entity.
     */
    val TILE_ENTITY_DATA_NOVA: ContextParamType<Compound, I>
        get() = tileEntityDataNova()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val TILE_ENTITY_DATA_NOVA = ContextParamType<Compound, Nothing>(
            novaKey("tile_entity_data_nova"),
            copy = Compound::copy
        )
        
        /**
         * Gets the param type for [TILE_ENTITY_DATA_NOVA].
         */
        fun <I : HasOptionalTileEntityData<I>> tileEntityDataNova() =
            TILE_ENTITY_DATA_NOVA as ContextParamType<Compound, I>
        
    }
    
}
