@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import java.util.*

/**
 * A [ContextIntention] that has optional parameters about who caused an action.
 */
interface HasOptionalSource<I : HasOptionalSource<I>> : ContextIntention<I> {
    
    /**
     * The [UUID] of the source of an action.
     *
     * Autofilled by
     * - [SOURCE_ENTITY]
     * - [SOURCE_TILE_ENTITY]
     */
    val SOURCE_UUID: ContextParamType<UUID, I>
    
    /**
     * The location of the source of an action.
     *
     * Autofilled by:
     * - [SOURCE_ENTITY]
     * - [SOURCE_TILE_ENTITY]
     */
    val SOURCE_LOCATION: ContextParamType<Location, I>
    
    /**
     * The world of the source of an action.
     *
     * Autofilled by:
     * - [SOURCE_LOCATION]
     *
     * Autofills: none
     */
    val SOURCE_WORLD: ContextParamType<World, I>
    
    /**
     * The direction that the source of an action is facing.
     *
     * Autofilled by:
     * - [SOURCE_LOCATION]
     *
     * Autofills: none
     */
    val SOURCE_DIRECTION: ContextParamType<Vector, I>
    
    /**
     * The entity that is the source of an action.
     *
     * Autofilled by:
     * - [SOURCE_LIVING_ENTITY]
     * - [SOURCE_PLAYER]
     */
    val SOURCE_ENTITY: ContextParamType<Entity, I>
    
    /**
     * The living entity that is the source of an action.
     *
     * Autofilled by:
     * - [SOURCE_ENTITY] if living entity
     */
    val SOURCE_LIVING_ENTITY: ContextParamType<LivingEntity, I>
    
    /**
     * The player that is the source of an action.
     *
     * Autofilled by:
     * - [SOURCE_LIVING_ENTITY] if player
     */
    val SOURCE_PLAYER: ContextParamType<Player, I>
    
    /**
     * The [TileEntity] that is the source of an action.
     *
     * Autofilled by: none
     */
    val SOURCE_TILE_ENTITY: ContextParamType<TileEntity, I>
    
    /**
     * The player that is either the direct source or responsible for the action.
     *
     * Autofilled by:
     * - [SOURCE_ENTITY] if offline player
     * - [SOURCE_TILE_ENTITY] if owner present
     */
    val RESPONSIBLE_PLAYER: ContextParamType<OfflinePlayer, I>
    
    companion object {
        
        /**
         * Creates a param type for [SOURCE_UUID].
         */
        fun <I : HasOptionalSource<I>> sourceUuid() =
            ContextParamType<UUID, I>(Key(Nova, "source_uuid"))
        
        /**
         * Creates a param type for [SOURCE_LOCATION].
         */
        fun <I : HasOptionalSource<I>> sourceLocation() =
            ContextParamType<Location, I>(Key(Nova, "source_location"), copy = Location::clone)
        
        /**
         * Creates a param type for [SOURCE_WORLD].
         */
        fun <I : HasOptionalSource<I>> sourceWorld() =
            ContextParamType<World, I>(Key(Nova, "source_world"))
        
        /**
         * Creates a param type for [SOURCE_DIRECTION].
         */
        fun <I : HasOptionalSource<I>> sourceDirection() =
            ContextParamType<Vector, I>(Key(Nova, "source_direction"), copy = Vector::clone)
        
        /**
         * Creates a param type for [SOURCE_ENTITY].
         */
        fun <I : HasOptionalSource<I>> sourceEntity() =
            ContextParamType<Entity, I>(Key(Nova, "source_entity"))
        
        /**
         * Creates a param type for [SOURCE_LIVING_ENTITY].
         */
        fun <I : HasOptionalSource<I>> sourceLivingEntity() =
            ContextParamType<LivingEntity, I>(Key(Nova, "source_living_entity"))
        
        /**
         * Creates a param type for [SOURCE_PLAYER].
         */
        fun <I : HasOptionalSource<I>> sourcePlayer() =
            ContextParamType<Player, I>(Key(Nova, "source_player"))
        
        /**
         * Creates a param type for [SOURCE_TILE_ENTITY].
         */
        fun <I : HasOptionalSource<I>> sourceTileEntity() =
            ContextParamType<TileEntity, I>(Key(Nova, "source_tile_entity"))
        
        /**
         * Creates a param type for [RESPONSIBLE_PLAYER].
         */
        fun <I : HasOptionalSource<I>> responsiblePlayer() =
            ContextParamType<OfflinePlayer, I>(Key(Nova, "responsible_player"))
        
        fun <I : HasOptionalSource<I>> applyDefaults(intention: HasOptionalSource<I>) = intention.apply {
            addAutofiller(SOURCE_UUID, Autofiller.from(SOURCE_ENTITY, Entity::getUniqueId))
            addAutofiller(SOURCE_UUID, Autofiller.from(SOURCE_TILE_ENTITY, TileEntity::uuid))
            addAutofiller(SOURCE_LOCATION, Autofiller.from(SOURCE_ENTITY, Entity::getLocation))
            addAutofiller(SOURCE_LOCATION, Autofiller.from(SOURCE_TILE_ENTITY) { it.pos.location })
            addAutofiller(SOURCE_WORLD, Autofiller.from(SOURCE_LOCATION, Location::getWorld))
            addAutofiller(SOURCE_DIRECTION, Autofiller.from(SOURCE_LOCATION, Location::getDirection))
            addAutofiller(SOURCE_ENTITY, Autofiller.from(SOURCE_PLAYER) { it })
            addAutofiller(SOURCE_ENTITY, Autofiller.from(SOURCE_LIVING_ENTITY) { it })
            addAutofiller(SOURCE_LIVING_ENTITY, Autofiller.from(SOURCE_ENTITY) { it as? LivingEntity })
            addAutofiller(SOURCE_PLAYER, Autofiller.from(SOURCE_ENTITY) { it as? Player })
            addAutofiller(RESPONSIBLE_PLAYER, Autofiller.from(SOURCE_ENTITY) { it as? OfflinePlayer })
            addAutofiller(RESPONSIBLE_PLAYER, Autofiller.from(SOURCE_TILE_ENTITY, TileEntity::owner))
        }
        
    }
    
}