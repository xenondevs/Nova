@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.bukkit.util.Vector
import org.joml.Vector3d
import xyz.xenondevs.nova.context.AbstractContextIntention
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.item.itemType
import java.util.*

/**
 * A [ContextIntention] for interacting with an entity.
 *
 * ## Autofillers
 *
 * Autofillers for the same target are queried in the order shown.
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [TARGET_ENTITY_UUID] | 1. | [TARGET_ENTITY] | [Entity.getUniqueId] |
 * |  |  |  |  |
 * | [TARGET_ENTITY_LOCATION] | 1. | [TARGET_ENTITY] | [Entity.getLocation] |
 * |  |  |  |  |
 * | [TARGET_ENTITY_WORLD] | 1. | [TARGET_ENTITY_LOCATION] | [Location.getWorld] |
 * |  |  |  |  |
 * | [TARGET_ENTITY_DIRECTION] | 1. | [TARGET_ENTITY_LOCATION] | [Location.getDirection] |
 * |  |  |  |  |
 * | [TARGET_LIVING_ENTITY] | 1. | [TARGET_ENTITY] | Only if target entity is living |
 * |  |  |  |  |
 * | [TARGET_PLAYER] | 1. | [TARGET_ENTITY] | Only if target entity is a player |
 * |  |  |  |  |
 * | [HELD_ITEM_STACK] | 1. | [HELD_ITEM_TYPE] | Default stack for the held item type |
 * | [HELD_ITEM_STACK] | 2. | [SOURCE_LIVING_ENTITY], [HELD_HAND] | Item held in the selected hand |
 * |  |  |  |  |
 * | [HELD_ITEM_TYPE] | 1. | [HELD_ITEM_STACK] | [ItemStack.itemType] |
 * |  |  |  |  |
 * | [SOURCE_UUID] | 1. | [SOURCE_ENTITY] | [Entity.getUniqueId] |
 * | [SOURCE_UUID] | 2. | [SOURCE_TILE_ENTITY] | [TileEntity.uuid] |
 * |  |  |  |  |
 * | [SOURCE_LOCATION] | 1. | [SOURCE_ENTITY] | [Entity.getLocation] |
 * | [SOURCE_LOCATION] | 2. | [SOURCE_TILE_ENTITY] | Tile entity's block location |
 * |  |  |  |  |
 * | [SOURCE_EYE_LOCATION] | 1. | [SOURCE_LIVING_ENTITY] | [LivingEntity.getEyeLocation] |
 * | [SOURCE_EYE_LOCATION] | 2. | [SOURCE_LOCATION] | Source location, unchanged |
 * |  |  |  |  |
 * | [SOURCE_WORLD] | 1. | [SOURCE_LOCATION] | [Location.getWorld] |
 * |  |  |  |  |
 * | [SOURCE_DIRECTION] | 1. | [SOURCE_LOCATION] | [Location.getDirection] |
 * |  |  |  |  |
 * | [SOURCE_ENTITY] | 1. | [SOURCE_PLAYER] | Source player, unchanged |
 * | [SOURCE_ENTITY] | 2. | [SOURCE_LIVING_ENTITY] | Source living entity, unchanged |
 * |  |  |  |  |
 * | [SOURCE_LIVING_ENTITY] | 1. | [SOURCE_ENTITY] | Only if source entity is living |
 * |  |  |  |  |
 * | [SOURCE_PLAYER] | 1. | [SOURCE_ENTITY] | Only if source entity is a player |
 * |  |  |  |  |
 * | [RESPONSIBLE_PLAYER] | 1. | [SOURCE_ENTITY] | Only if source entity is an [OfflinePlayer] |
 * | [RESPONSIBLE_PLAYER] | 2. | [SOURCE_TILE_ENTITY] | [TileEntity.owner] |
 * |  |  |  |  |
 * | [INTERACT_LOCATION] | 1. | [SOURCE_EYE_LOCATION], [TARGET_ENTITY] | Gaze hit on the target, relative to its location |
 */
object EntityInteract : AbstractContextIntention<EntityInteract>() {
    
    /**
     * The [UUID] of the target entity.
     */
    val TARGET_ENTITY_UUID = addRequiredParamType<UUID>()
    
    /**
     * The target entity.
     */
    val TARGET_ENTITY = addRequiredParamType<Entity>()
    
    /**
     * The location of the target entity.
     */
    val TARGET_ENTITY_LOCATION = addRequiredParamType(copy = Location::clone)
    
    /**
     * The world of the target entity.
     */
    val TARGET_ENTITY_WORLD = addRequiredParamType<World>()
    
    /**
     * The direction that the target entity is facing.
     */
    val TARGET_ENTITY_DIRECTION = addRequiredParamType(copy = Vector::clone)
    
    /**
     * The target entity as a living entity.
     */
    val TARGET_LIVING_ENTITY = addOptionalParamType<LivingEntity>()
    
    /**
     * The target entity as a player.
     */
    val TARGET_PLAYER = addOptionalParamType<Player>()
    
    /**
     * The [UUID] of the source of the interaction.
     */
    val SOURCE_UUID = addOptionalParamType<UUID>()
    
    /**
     * The location of the source of the interaction.
     */
    val SOURCE_LOCATION = addOptionalParamType(copy = Location::clone)
    
    /**
     * The eye location of the source of the interaction.
     */
    val SOURCE_EYE_LOCATION = addOptionalParamType(copy = Location::clone)
    
    /**
     * The world of the source of the interaction.
     */
    val SOURCE_WORLD = addOptionalParamType<World>()
    
    /**
     * The direction that the source of the interaction is facing.
     */
    val SOURCE_DIRECTION = addOptionalParamType(copy = Vector::clone)
    
    /**
     * The entity that is the source of the interaction.
     */
    val SOURCE_ENTITY = addOptionalParamType<Entity>()
    
    /**
     * The living entity that is the source of the interaction.
     */
    val SOURCE_LIVING_ENTITY = addOptionalParamType<LivingEntity>()
    
    /**
     * The player that is the source of the interaction.
     */
    val SOURCE_PLAYER = addOptionalParamType<Player>()
    
    /**
     * The [TileEntity] that is the source of the interaction.
     */
    val SOURCE_TILE_ENTITY = addOptionalParamType<TileEntity>()
    
    /**
     * The player that either interacts with the entity or is responsible for the interaction.
     */
    val RESPONSIBLE_PLAYER = addOptionalParamType<OfflinePlayer>()
    
    /**
     * The item stack held during the interaction.
     * Defaults to [ItemStack.empty].
     */
    val HELD_ITEM_STACK = addDefaultingParamType(default = ItemStack.empty(), copy = ItemStack::clone)
    
    /**
     * The type of the item held during the interaction.
     * Defaults to [ItemType.AIR].
     */
    val HELD_ITEM_TYPE = addDefaultingParamType(default = ItemType.AIR)
    
    /**
     * The hand used to interact with the entity.
     */
    val HELD_HAND = addOptionalParamType(validate = EquipmentSlot::isHand)
    
    /**
     * The location that was interacted with, relative to the position of the target entity.
     */
    val INTERACT_LOCATION = addOptionalParamType<Vector3d>()
    
    init {
        addAutofiller(TARGET_ENTITY_UUID, Autofiller.from(TARGET_ENTITY, Entity::getUniqueId))
        
        addAutofiller(TARGET_ENTITY_LOCATION, Autofiller.from(TARGET_ENTITY, Entity::getLocation))
        
        addAutofiller(TARGET_ENTITY_WORLD, Autofiller.from(TARGET_ENTITY_LOCATION, Location::getWorld))
        
        addAutofiller(TARGET_ENTITY_DIRECTION, Autofiller.from(TARGET_ENTITY_LOCATION, Location::getDirection))
        
        addAutofiller(TARGET_LIVING_ENTITY, Autofiller.from(TARGET_ENTITY) { it as? LivingEntity })
        
        addAutofiller(TARGET_PLAYER, Autofiller.from(TARGET_ENTITY) { it as? Player })
        
        addAutofiller(HELD_ITEM_STACK, Autofiller.from(HELD_ITEM_TYPE, ItemType::createItemStack))
        addAutofiller(HELD_ITEM_STACK, Autofiller.from(SOURCE_LIVING_ENTITY, HELD_HAND) { entity, hand -> entity.equipment?.getItem(hand) })
        
        addAutofiller(HELD_ITEM_TYPE, Autofiller.from(HELD_ITEM_STACK, ItemStack::itemType))
        
        addAutofiller(SOURCE_UUID, Autofiller.from(SOURCE_ENTITY, Entity::getUniqueId))
        addAutofiller(SOURCE_UUID, Autofiller.from(SOURCE_TILE_ENTITY, TileEntity::uuid))
        
        addAutofiller(SOURCE_LOCATION, Autofiller.from(SOURCE_ENTITY, Entity::getLocation))
        addAutofiller(SOURCE_LOCATION, Autofiller.from(SOURCE_TILE_ENTITY) { it.block.location })
        
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
        
        addAutofiller(INTERACT_LOCATION, Autofiller.from(SOURCE_EYE_LOCATION, TARGET_ENTITY, EntityUtils::computeInteractionLocation))
    }
    
}