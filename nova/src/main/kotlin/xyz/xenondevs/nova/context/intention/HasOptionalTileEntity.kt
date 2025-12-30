@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import org.bukkit.block.TileState
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.intention.HasOptionalTileEntity.Companion.TILE_ENTITY_DATA_NOVA
import xyz.xenondevs.nova.context.intention.HasOptionalTileEntity.Companion.TILE_ENTITY_NOVA
import xyz.xenondevs.nova.context.intention.HasOptionalTileEntity.Companion.TILE_ENTITY_VANILLA
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.format.WorldDataManager

/**
 * A [ContextIntention] that has optional parameters about the tile entity of a block.
 *
 * ## Autofillers
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [TILE_ENTITY_NOVA] | 1. | [BLOCK_POS] | |
 * | [TILE_ENTITY_DATA_NOVA] | 1. | [TILE_ENTITY_NOVA] | |
 */
interface HasOptionalTileEntity<I : HasOptionalTileEntity<I>> : HasRequiredBlock<I> {
    
    /**
     * The nova tile-entity of a block.
     */
    val TILE_ENTITY_NOVA: ContextParamType<TileEntity, I>
        get() = tileEntityNova()
    
    /**
     * The tile-entity data of a nova tile-entity.
     */
    val TILE_ENTITY_DATA_NOVA: ContextParamType<Compound, I>
        get() = tileEntityDataNova()
    
    /**
     * The vanilla tile-entity of a block.
     */
    val TILE_ENTITY_VANILLA: ContextParamType<TileState, I>
        get() = tileEntityVanilla()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val TILE_ENTITY_NOVA = ContextParamType<TileEntity, Nothing>(Key(Nova, "tile_entity_nova"))
        private val TILE_ENTITY_DATA_NOVA = ContextParamType<Compound, Nothing>(Key(Nova, "tile_entity_data_nova"), copy = Compound::copy)
        private val TILE_ENTITY_VANILLA = ContextParamType<TileState, Nothing>(Key(Nova, "tile_entity_vanilla"), copy = { it.copy() as TileState })
        
        /**
         * Gets the param type for [TILE_ENTITY_NOVA].
         */
        fun <I : HasOptionalTileEntity<I>> tileEntityNova() =
            TILE_ENTITY_NOVA as ContextParamType<TileEntity, I>
        
        /**
         * Gets the param type for [TILE_ENTITY_DATA_NOVA].
         */
        fun <I : HasOptionalTileEntity<I>> tileEntityDataNova() =
            TILE_ENTITY_DATA_NOVA as ContextParamType<Compound, I>
        
        /**
         * Gets the param type for [TILE_ENTITY_VANILLA].
         */
        fun <I : HasOptionalTileEntity<I>> tileEntityVanilla() =
            TILE_ENTITY_VANILLA as ContextParamType<TileState, I>
        
        /**
         * Applies the default autofillers on [intention].
         */
        fun <I : HasOptionalTileEntity<I>> applyDefaults(intention: HasOptionalTileEntity<I>) = intention.apply {
            addAutofiller(TILE_ENTITY_NOVA, Autofiller.from(BLOCK_POS, WorldDataManager::getTileEntity))
            addAutofiller(TILE_ENTITY_DATA_NOVA, Autofiller.from(TILE_ENTITY_NOVA, TileEntity::data))
            addAutofiller(TILE_ENTITY_VANILLA, Autofiller.from(BLOCK_POS) { it.block.state as? TileState })
        }
        
    }
    
}