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
import xyz.xenondevs.nova.context.intention.HasOptionalSource.Companion.RESPONSIBLE_PLAYER
import xyz.xenondevs.nova.context.intention.HasOptionalSource.Companion.SOURCE_DIRECTION
import xyz.xenondevs.nova.context.intention.HasOptionalSource.Companion.SOURCE_ENTITY
import xyz.xenondevs.nova.context.intention.HasOptionalSource.Companion.SOURCE_EYE_LOCATION
import xyz.xenondevs.nova.context.intention.HasOptionalSource.Companion.SOURCE_LIVING_ENTITY
import xyz.xenondevs.nova.context.intention.HasOptionalSource.Companion.SOURCE_LOCATION
import xyz.xenondevs.nova.context.intention.HasOptionalSource.Companion.SOURCE_PLAYER
import xyz.xenondevs.nova.context.intention.HasOptionalSource.Companion.SOURCE_TILE_ENTITY
import xyz.xenondevs.nova.context.intention.HasOptionalSource.Companion.SOURCE_UUID
import xyz.xenondevs.nova.context.intention.HasOptionalSource.Companion.SOURCE_WORLD
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import java.util.*

/**
 * A [ContextIntention] that has optional parameters about who caused an action.
 *
 * ## Autofillers
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [SOURCE_UUID] | 1. | [SOURCE_ENTITY] | |
 * | | 2. | [SOURCE_TILE_ENTITY] | |
 * | [SOURCE_LOCATION] | 1. | [SOURCE_ENTITY] | |
 * | | 2. | [SOURCE_TILE_ENTITY] | |
 * | [SOURCE_EYE_LOCATION] | 1. | [SOURCE_LIVING_ENTITY] | |
 * | | 2. | [SOURCE_LOCATION] | |
 * | [SOURCE_WORLD] | 1. | [SOURCE_LOCATION] | |
 * | [SOURCE_DIRECTION] | 1. | [SOURCE_LOCATION] | |
 * | [SOURCE_ENTITY] | 1. | [SOURCE_PLAYER] | |
 * | | 2. | [SOURCE_LIVING_ENTITY] | |
 * | [SOURCE_LIVING_ENTITY] | 1. | [SOURCE_ENTITY] | Only if living entity |
 * | [SOURCE_PLAYER] | 1. | [SOURCE_ENTITY] | Only if player |
 * | [RESPONSIBLE_PLAYER] | 1. | [SOURCE_ENTITY] | Only if offline player |
 * | | 2. | [SOURCE_TILE_ENTITY] | Only if owner present |
 */
interface HasOptionalSource<I : HasOptionalSource<I>> : ContextIntention<I> {
    
    /**
     * The [UUID] of the source of an action.
     */
    val SOURCE_UUID: ContextParamType<UUID, I>
        get() = sourceUuid()
    
    /**
     * The location of the source of an action.
     */
    val SOURCE_LOCATION: ContextParamType<Location, I>
        get() = sourceLocation()
    
    /**
     * The eye location of the source of an action.
     */
    val SOURCE_EYE_LOCATION: ContextParamType<Location, I>
        get() = sourceEyeLocation()
    
    /**
     * The world of the source of an action.
     */
    val SOURCE_WORLD: ContextParamType<World, I>
        get() = sourceWorld()
    
    /**
     * The direction that the source of an action is facing.
     */
    val SOURCE_DIRECTION: ContextParamType<Vector, I>
        get() = sourceDirection()
    
    /**
     * The entity that is the source of an action.
     */
    val SOURCE_ENTITY: ContextParamType<Entity, I>
        get() = sourceEntity()
    
    /**
     * The living entity that is the source of an action.
     */
    val SOURCE_LIVING_ENTITY: ContextParamType<LivingEntity, I>
        get() = sourceLivingEntity()
    
    /**
     * The player that is the source of an action.
     */
    val SOURCE_PLAYER: ContextParamType<Player, I>
        get() = sourcePlayer()
    
    /**
     * The [TileEntity] that is the source of an action.
     */
    val SOURCE_TILE_ENTITY: ContextParamType<TileEntity, I>
        get() = sourceTileEntity()
    
    /**
     * The player that is either the direct source or responsible for the action.
     */
    val RESPONSIBLE_PLAYER: ContextParamType<OfflinePlayer, I>
        get() = responsiblePlayer()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val SOURCE_UUID = ContextParamType<UUID, Nothing>(Key(Nova, "source_uuid"))
        private val SOURCE_LOCATION = ContextParamType<Location, Nothing>(Key(Nova, "source_location"), copy = Location::clone)
        private val SOURCE_EYE_LOCATION = ContextParamType<Location, Nothing>(Key(Nova, "source_eye_location"), copy = Location::clone)
        private val SOURCE_WORLD = ContextParamType<World, Nothing>(Key(Nova, "source_world"))
        private val SOURCE_DIRECTION = ContextParamType<Vector, Nothing>(Key(Nova, "source_direction"), copy = Vector::clone)
        private val SOURCE_ENTITY = ContextParamType<Entity, Nothing>(Key(Nova, "source_entity"))
        private val SOURCE_LIVING_ENTITY = ContextParamType<LivingEntity, Nothing>(Key(Nova, "source_living_entity"))
        private val SOURCE_PLAYER = ContextParamType<Player, Nothing>(Key(Nova, "source_player"))
        private val SOURCE_TILE_ENTITY = ContextParamType<TileEntity, Nothing>(Key(Nova, "source_tile_entity"))
        private val RESPONSIBLE_PLAYER = ContextParamType<OfflinePlayer, Nothing>(Key(Nova, "responsible_player"))
        
        /**
         * Gets the param type for [SOURCE_UUID].
         */
        fun <I : HasOptionalSource<I>> sourceUuid() =
            SOURCE_UUID as ContextParamType<UUID, I>
        
        /**
         * Gets the param type for [SOURCE_LOCATION].
         */
        fun <I : HasOptionalSource<I>> sourceLocation() =
            SOURCE_LOCATION as ContextParamType<Location, I>
        
        /**
         * Gets the param type for [SOURCE_EYE_LOCATION].
         */
        fun <I : HasOptionalSource<I>> sourceEyeLocation() =
            SOURCE_EYE_LOCATION as ContextParamType<Location, I>
        
        /**
         * Gets the param type for [SOURCE_WORLD].
         */
        fun <I : HasOptionalSource<I>> sourceWorld() =
            SOURCE_WORLD as ContextParamType<World, I>
        
        /**
         * Gets the param type for [SOURCE_DIRECTION].
         */
        fun <I : HasOptionalSource<I>> sourceDirection() =
            SOURCE_DIRECTION as ContextParamType<Vector, I>
        
        /**
         * Gets the param type for [SOURCE_ENTITY].
         */
        fun <I : HasOptionalSource<I>> sourceEntity() =
            SOURCE_ENTITY as ContextParamType<Entity, I>
        
        /**
         * Gets the param type for [SOURCE_LIVING_ENTITY].
         */
        fun <I : HasOptionalSource<I>> sourceLivingEntity() =
            SOURCE_LIVING_ENTITY as ContextParamType<LivingEntity, I>
        
        /**
         * Gets the param type for [SOURCE_PLAYER].
         */
        fun <I : HasOptionalSource<I>> sourcePlayer() =
            SOURCE_PLAYER as ContextParamType<Player, I>
        
        /**
         * Gets the param type for [SOURCE_TILE_ENTITY].
         */
        fun <I : HasOptionalSource<I>> sourceTileEntity() =
            SOURCE_TILE_ENTITY as ContextParamType<TileEntity, I>
        
        /**
         * Gets the param type for [RESPONSIBLE_PLAYER].
         */
        fun <I : HasOptionalSource<I>> responsiblePlayer() =
            RESPONSIBLE_PLAYER as ContextParamType<OfflinePlayer, I>
        
        fun <I : HasOptionalSource<I>> applyDefaults(intention: HasOptionalSource<I>) = intention.apply {
            addAutofiller(SOURCE_UUID, Autofiller.from(SOURCE_ENTITY, Entity::getUniqueId))
            addAutofiller(SOURCE_UUID, Autofiller.from(SOURCE_TILE_ENTITY, TileEntity::uuid))
            addAutofiller(SOURCE_LOCATION, Autofiller.from(SOURCE_ENTITY, Entity::getLocation))
            addAutofiller(SOURCE_LOCATION, Autofiller.from(SOURCE_TILE_ENTITY) { it.pos.location })
            addAutofiller(SOURCE_EYE_LOCATION, Autofiller.from(SOURCE_LIVING_ENTITY, LivingEntity::getEyeLocation))
            addAutofiller(SOURCE_EYE_LOCATION, Autofiller.from(SOURCE_LOCATION) { it })
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