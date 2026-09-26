@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockType
import org.bukkit.block.TileState
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import org.bukkit.util.Vector
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.context.AbstractContextIntention
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.isNova
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.item.itemType
import java.util.*

/**
 * A [ContextIntention] for clicking on a block.
 *
 * ## Autofillers
 *
 * Autofillers for the same target are queried in the order shown.
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [BLOCK_WORLD] | 1. | [BLOCK] | [Block.getWorld] |
 * |  |  |  |  |
 * | [BLOCK_TYPE] | 1. | [BLOCK_STATE] | [BlockData.blockType] |
 * |  |  |  |  |
 * | [TILE_ENTITY_NOVA] | 1. | [BLOCK] | [Block.novaTileEntity] |
 * |  |  |  |  |
 * | [TILE_ENTITY_DATA_NOVA] | 1. | [TILE_ENTITY_NOVA] | [TileEntity.data] |
 * |  |  |  |  |
 * | [TILE_ENTITY_VANILLA] | 1. | [BLOCK] | Only for non-Nova blocks with a [TileState] |
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
 * | [CLICKED_BLOCK_FACE] | 1. | [SOURCE_PLAYER] | Face the player is looking at |
 * |  |  |  |  |
 * | [HELD_ITEM_STACK] | 1. | [HELD_ITEM_TYPE] | Default stack for the held item type |
 * | [HELD_ITEM_STACK] | 2. | [SOURCE_LIVING_ENTITY], [HELD_HAND] | Item held in the selected hand |
 * |  |  |  |  |
 * | [HELD_ITEM_TYPE] | 1. | [HELD_ITEM_STACK] | [ItemStack.itemType] |
 * |  |  |  |  |
 * | [BLOCK_STATE] | 1. | [BLOCK] | [Block.getBlockData] |
 */
object BlockInteract : AbstractContextIntention<BlockInteract>() {
    
    /**
     * The block being interacted with.
     */
    val BLOCK = addRequiredParamType<Block>()
    
    /**
     * The world of the block being interacted with.
     */
    val BLOCK_WORLD = addRequiredParamType<World>()
    
    /**
     * The type of the block being interacted with.
     */
    val BLOCK_TYPE = addRequiredParamType<BlockType>()
    
    /**
     * The block data (block state) of the block being interacted with.
     */
    val BLOCK_STATE = addRequiredParamType<BlockData>()
    
    /**
     * The data of the block being interacted with, if it is a Nova tile entity.
     */
    val TILE_ENTITY_DATA_NOVA = addOptionalParamType(copy = Compound::copy)
    
    /**
     * The [TileEntity] instance of the block being interacted with, if it is a Nova tile entity.
     */
    val TILE_ENTITY_NOVA = addOptionalParamType<TileEntity>()
    
    /**
     * The [TileState] instance of the block being interacted with, if it is a vanilla tile entity.
     */
    val TILE_ENTITY_VANILLA = addOptionalParamType<TileState>(copy = { it.copy() as TileState })
    
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
     * The player that either interacts with the block or is responsible for the interaction.
     */
    val RESPONSIBLE_PLAYER = addOptionalParamType<OfflinePlayer>()
    
    /**
     * The face of the block that was clicked.
     */
    val CLICKED_BLOCK_FACE = addOptionalParamType<BlockFace>()
    
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
     * The hand used to interact with the block.
     */
    val HELD_HAND = addOptionalParamType(validate = EquipmentSlot::isHand)
    
    /**
     * Whether the data of the block should be included for creative-pick block interactions.
     * Defaults to `false`.
     */
    val INCLUDE_DATA = addDefaultingParamType(default = false)
    
    init {
        addAutofiller(BLOCK_WORLD, Autofiller.from(BLOCK, Block::getWorld))
        
        addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_STATE) { it.blockType })
        
        addAutofiller(TILE_ENTITY_NOVA, Autofiller.from(BLOCK, Block::novaTileEntity))
        
        addAutofiller(TILE_ENTITY_DATA_NOVA, Autofiller.from(TILE_ENTITY_NOVA, TileEntity::data))
        
        addAutofiller(TILE_ENTITY_VANILLA, Autofiller.from(BLOCK) { if (it.blockType.isNova) null else it.state as? TileState })
        
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
        
        addAutofiller(CLICKED_BLOCK_FACE, Autofiller.from(SOURCE_PLAYER) { BlockFaceUtils.determineBlockFaceLookingAt(it.eyeLocation) })
        
        addAutofiller(HELD_ITEM_STACK, Autofiller.from(HELD_ITEM_TYPE, ItemType::createItemStack))
        addAutofiller(HELD_ITEM_STACK, Autofiller.from(SOURCE_LIVING_ENTITY, HELD_HAND) { entity, hand -> entity.equipment?.getItem(hand) })
        
        addAutofiller(HELD_ITEM_TYPE, Autofiller.from(HELD_ITEM_STACK, ItemStack::itemType))
        
        addAutofiller(BLOCK_STATE, Autofiller.from(BLOCK, Block::getBlockData))
    }
    
}
