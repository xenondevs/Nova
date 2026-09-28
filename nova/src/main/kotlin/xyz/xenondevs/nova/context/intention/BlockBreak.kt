@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.GameMode
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
import xyz.xenondevs.nova.world.block.BlockUpdateFlags
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.isNova
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.item.itemType
import java.util.*

/**
 * A [ContextIntention] for when a block is broken.
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
 * | [HELD_ITEM_STACK] | 2. | [SOURCE_LIVING_ENTITY] | Item held in the main hand |
 * |  |  |  |  |
 * | [HELD_ITEM_TYPE] | 1. | [HELD_ITEM_STACK] | [ItemStack.itemType] |
 * |  |  |  |  |
 * | [TOOL_ITEM_STACK] | 1. | [HELD_ITEM_STACK] | Only if the held item has a tool component |
 * |  |  |  |  |
 * | [BLOCK_DROPS] | 1. | [BLOCK], [TOOL_ITEM_STACK], [SOURCE_PLAYER] | Non-creative player and tool sufficient for drops |
 * | [BLOCK_DROPS] | 2. | [BLOCK], [SOURCE_PLAYER] | Non-creative player and empty hand sufficient for drops |
 * | [BLOCK_DROPS] | 3. | [BLOCK] | Empty hand sufficient for drops |
 * |  |  |  |  |
 * | [BLOCK_EXP_DROPS] | 1. | [BLOCK_DROPS] | Same as [BLOCK_DROPS] |
 * |  |  |  |  |
 * | [BLOCK_STATE] | 1. | [BLOCK] | [Block.getBlockData] |
 */
object BlockBreak : AbstractContextIntention<BlockBreak>() {
    
    /**
     * The block position that is being broken.
     */
    val BLOCK = addRequiredParamType<Block>()
    
    /**
     * The world of the block being broken.
     */
    val BLOCK_WORLD = addRequiredParamType<World>()
    
    /**
     * The type of the block being broken.
     */
    val BLOCK_TYPE = addRequiredParamType<BlockType>()
    
    /**
     * The block data (block state) of the block being broken.
     */
    val BLOCK_STATE = addRequiredParamType<BlockData>()
    
    /**
     * The [BlockUpdateFlags] that are used for this action.
     */
    val BLOCK_UPDATE_FLAGS = addDefaultingParamType(default = BlockUpdateFlags.ALL)
    
    /**
     * The data of the [TileEntity] being broken, if it is a nova tile-entity.
     */
    val TILE_ENTITY_DATA_NOVA = addOptionalParamType(copy = Compound::copy)
    
    /**
     * The [TileEntity] instance of the block being broken, if it is a nova tile-entity.
     */
    val TILE_ENTITY_NOVA = addOptionalParamType<TileEntity>()
    
    /**
     * The [TileState] instance of the block being broken, if it is a vanilla tile-entity.
     */
    val TILE_ENTITY_VANILLA = addOptionalParamType<TileState>(copy = { it.copy() as TileState })
    
    /**
     * The [UUID] of the source of the block break.
     */
    val SOURCE_UUID = addOptionalParamType<UUID>()
    
    /**
     * The location of the source of the block break.
     */
    val SOURCE_LOCATION = addOptionalParamType(copy = Location::clone)
    
    /**
     * The eye location of the source of the block break.
     */
    val SOURCE_EYE_LOCATION = addOptionalParamType(copy = Location::clone)
    
    /**
     * The world of the source of the block break.
     */
    val SOURCE_WORLD = addOptionalParamType<World>()
    
    /**
     * The direction that the source of the block break is facing.
     */
    val SOURCE_DIRECTION = addOptionalParamType(copy = Vector::clone)
    
    /**
     * The entity that is the source of the block break.
     */
    val SOURCE_ENTITY = addOptionalParamType<Entity>()
    
    /**
     * The living entity that is the source of the block break.
     */
    val SOURCE_LIVING_ENTITY = addOptionalParamType<LivingEntity>()
    
    /**
     * The player that is the source of the block break.
     */
    val SOURCE_PLAYER = addOptionalParamType<Player>()
    
    /**
     * The [TileEntity] that is the source of the block break.
     */
    val SOURCE_TILE_ENTITY = addOptionalParamType<TileEntity>()
    
    /**
     * The player that either breaks the block or is responsible for the break.
     */
    val RESPONSIBLE_PLAYER = addOptionalParamType<OfflinePlayer>()
    
    /**
     * The [BlockFace] that is being clicked while breaking the block.
     */
    val CLICKED_BLOCK_FACE = addOptionalParamType<BlockFace>()
    
    /**
     * The item stack held in the main hand while breaking the block, whether or not it is a tool.
     * Defaults to [ItemStack.empty].
     */
    val HELD_ITEM_STACK = addDefaultingParamType(default = ItemStack.empty(), copy = ItemStack::clone)
    
    /**
     * The type of the item held in the main hand while breaking the block.
     * Defaults to [ItemType.AIR].
     */
    val HELD_ITEM_TYPE = addDefaultingParamType(default = ItemType.AIR)
    
    /**
     * The hand used to break the block. Block breaking always uses the main hand.
     * Defaults to [EquipmentSlot.HAND].
     *
     * This parameter is retained for compatibility and does not affect [HELD_ITEM_STACK].
     */
    @Deprecated("Block breaking always uses the main hand")
    val HELD_HAND = addDefaultingParamType(default = EquipmentSlot.HAND, validate = EquipmentSlot::isHand)
    
    /**
     * The [ItemStack] used as the breaking tool for drops, experience, and protection checks.
     * This may differ from [HELD_ITEM_STACK] for a programmatic break; it is autofilled from
     * the held item only when that item has a tool component.
     * Defaults to [ItemStack.empty].
     */
    val TOOL_ITEM_STACK = addDefaultingParamType(default = ItemStack.empty(), copy = ItemStack::clone)
    
    /**
     * Whether block drops should be dropped.
     * Defaults to `false`.
     *
     * @see BLOCK_STORAGE_DROPS
     * @see BLOCK_EXP_DROPS
     */
    val BLOCK_DROPS = addDefaultingParamType(default = false)
    
    /**
     * Whether block storage drops should be dropped.
     * Defaults to `true`
     *
     * @see BLOCK_DROPS
     * @see BLOCK_EXP_DROPS
     */
    val BLOCK_STORAGE_DROPS = addDefaultingParamType(default = true)
    
    /**
     * Whether block exp orbs should be spawned.
     * Defaults to `false`
     *
     * @see BLOCK_DROPS
     * @see BLOCK_STORAGE_DROPS
     */
    val BLOCK_EXP_DROPS = addDefaultingParamType(default = false)
    
    /**
     * Whether block break effects should be played.
     * Defaults to `true`
     */
    val BLOCK_BREAK_EFFECTS = addDefaultingParamType(default = true)
    
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
        addAutofiller(HELD_ITEM_STACK, Autofiller.from(SOURCE_LIVING_ENTITY) { entity -> entity.equipment?.itemInMainHand }) // block breaking is always main-hand
        
        addAutofiller(HELD_ITEM_TYPE, Autofiller.from(HELD_ITEM_STACK, ItemStack::itemType))
        
        addAutofiller(TOOL_ITEM_STACK, Autofiller.from(HELD_ITEM_STACK) { if (it.hasData(DataComponentTypes.TOOL)) it else null })
        
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK, TOOL_ITEM_STACK, SOURCE_PLAYER) { block, tool, player -> player.gameMode != GameMode.CREATIVE && block.isPreferredTool(tool) })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK, SOURCE_PLAYER) { block, player -> player.gameMode != GameMode.CREATIVE && block.isPreferredTool(ItemStack.empty()) })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK) { block -> block.isPreferredTool(ItemStack.empty()) })
        
        addAutofiller(BLOCK_EXP_DROPS, Autofiller.from(BLOCK_DROPS) { it })
        
        addAutofiller(BLOCK_STATE, Autofiller.from(BLOCK) { it.blockData })
    }
    
}
