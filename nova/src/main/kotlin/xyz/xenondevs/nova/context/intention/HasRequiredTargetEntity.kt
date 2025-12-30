@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.RequiredContextParamType
import xyz.xenondevs.nova.context.intention.HasRequiredTargetEntity.Companion.TARGET_ENTITY
import xyz.xenondevs.nova.context.intention.HasRequiredTargetEntity.Companion.TARGET_ENTITY_DIRECTION
import xyz.xenondevs.nova.context.intention.HasRequiredTargetEntity.Companion.TARGET_ENTITY_LOCATION
import xyz.xenondevs.nova.context.intention.HasRequiredTargetEntity.Companion.TARGET_ENTITY_UUID
import xyz.xenondevs.nova.context.intention.HasRequiredTargetEntity.Companion.TARGET_ENTITY_WORLD
import xyz.xenondevs.nova.context.intention.HasRequiredTargetEntity.Companion.TARGET_LIVING_ENTITY
import xyz.xenondevs.nova.context.intention.HasRequiredTargetEntity.Companion.TARGET_PLAYER
import xyz.xenondevs.nova.util.Key
import java.util.*

/**
 * A [ContextIntention] that has required parameters about a target entity.
 *
 * ## Autofillers
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [TARGET_ENTITY_UUID] | 1. | [TARGET_ENTITY] | |
 * | [TARGET_ENTITY_LOCATION] | 1. | [TARGET_ENTITY] | |
 * | [TARGET_ENTITY_WORLD] | 1. | [TARGET_ENTITY_LOCATION] | |
 * | [TARGET_ENTITY_DIRECTION] | 1. | [TARGET_ENTITY_LOCATION] | |
 * | [TARGET_LIVING_ENTITY] | 1. | [TARGET_ENTITY] | Only if living entity |
 * | [TARGET_PLAYER] | 1. | [TARGET_ENTITY] | Only if player |
 */
interface HasRequiredTargetEntity<I : HasRequiredTargetEntity<I>> : ContextIntention<I> {
    
    /**
     * The [UUID] of the target entity.
     */
    val TARGET_ENTITY_UUID: RequiredContextParamType<UUID, I>
        get() = targetEntityUuid()
    
    /**
     * The target entity.
     */
    val TARGET_ENTITY: RequiredContextParamType<Entity, I>
        get() = targetEntity()
    
    /**
     * The location of the target entity.
     */
    val TARGET_ENTITY_LOCATION: RequiredContextParamType<Location, I>
        get() = targetEntityLocation()
    
    /**
     * The world of the target entity.
     */
    val TARGET_ENTITY_WORLD: RequiredContextParamType<World, I>
        get() = targetEntityWorld()
    
    /**
     * The direction that the target entity is facing.
     */
    val TARGET_ENTITY_DIRECTION: RequiredContextParamType<Vector, I>
        get() = targetEntityDirection()
    
    /**
     * The target entity as a living entity.
     */
    val TARGET_LIVING_ENTITY: ContextParamType<LivingEntity, I>
        get() = targetLivingEntity()
    
    /**
     * The target entity as a player.
     */
    val TARGET_PLAYER: ContextParamType<Player, I>
        get() = targetPlayer()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val TARGET_ENTITY_UUID = RequiredContextParamType<UUID, Nothing>(Key(Nova, "target_entity_uuid"))
        private val TARGET_ENTITY = RequiredContextParamType<Entity, Nothing>(Key(Nova, "target_entity"))
        private val TARGET_ENTITY_LOCATION = RequiredContextParamType<Location, Nothing>(Key(Nova, "target_entity_location"), copy = Location::clone)
        private val TARGET_ENTITY_WORLD = RequiredContextParamType<World, Nothing>(Key(Nova, "target_entity_world"))
        private val TARGET_ENTITY_DIRECTION = RequiredContextParamType<Vector, Nothing>(Key(Nova, "target_entity_direction"), copy = Vector::clone)
        private val TARGET_LIVING_ENTITY = ContextParamType<LivingEntity, Nothing>(Key(Nova, "target_living_entity"))
        private val TARGET_PLAYER = ContextParamType<Player, Nothing>(Key(Nova, "target_player"))
        
        /**
         * Gets the param type for [TARGET_ENTITY_UUID].
         */
        fun <I : HasRequiredTargetEntity<I>> targetEntityUuid() =
            TARGET_ENTITY_UUID as RequiredContextParamType<UUID, I>
        
        /**
         * Gets the param type for [TARGET_ENTITY].
         */
        fun <I : HasRequiredTargetEntity<I>> targetEntity() =
            TARGET_ENTITY as RequiredContextParamType<Entity, I>
        
        /**
         * Gets the param type for [TARGET_ENTITY_LOCATION].
         */
        fun <I : HasRequiredTargetEntity<I>> targetEntityLocation() =
            TARGET_ENTITY_LOCATION as RequiredContextParamType<Location, I>
        
        /**
         * Gets the param type for [TARGET_ENTITY_WORLD].
         */
        fun <I : HasRequiredTargetEntity<I>> targetEntityWorld() =
            TARGET_ENTITY_WORLD as RequiredContextParamType<World, I>
        
        /**
         * Gets the param type for [TARGET_ENTITY_DIRECTION].
         */
        fun <I : HasRequiredTargetEntity<I>> targetEntityDirection() =
            TARGET_ENTITY_DIRECTION as RequiredContextParamType<Vector, I>
        
        /**
         * Gets the param type for [TARGET_LIVING_ENTITY].
         */
        fun <I : HasRequiredTargetEntity<I>> targetLivingEntity() =
            TARGET_LIVING_ENTITY as ContextParamType<LivingEntity, I>
        
        /**
         * Gets the param type for [TARGET_PLAYER].
         */
        fun <I : HasRequiredTargetEntity<I>> targetPlayer() =
            TARGET_PLAYER as ContextParamType<Player, I>
        
        /**
         * Applies the default required properties and autofillers on [intention].
         */
        fun <I : HasRequiredTargetEntity<I>> applyDefaults(intention: HasRequiredTargetEntity<I>) = intention.apply {
            require(TARGET_ENTITY)
            require(TARGET_ENTITY_UUID)
            require(TARGET_ENTITY_LOCATION)
            require(TARGET_ENTITY_WORLD)
            require(TARGET_ENTITY_DIRECTION)
            addAutofiller(TARGET_ENTITY_UUID, Autofiller.from(TARGET_ENTITY, Entity::getUniqueId))
            addAutofiller(TARGET_ENTITY_LOCATION, Autofiller.from(TARGET_ENTITY, Entity::getLocation))
            addAutofiller(TARGET_ENTITY_WORLD, Autofiller.from(TARGET_ENTITY_LOCATION, Location::getWorld))
            addAutofiller(TARGET_ENTITY_DIRECTION, Autofiller.from(TARGET_ENTITY_LOCATION, Location::getDirection))
            addAutofiller(TARGET_LIVING_ENTITY, Autofiller.from(TARGET_ENTITY) { it as? LivingEntity })
            addAutofiller(TARGET_PLAYER, Autofiller.from(TARGET_ENTITY) { it as? Player })
        }
        
    }
    
}