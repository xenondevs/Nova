@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.data.context.param

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.Location
import xyz.xenondevs.nova.util.bukkitMaterial
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.nmsBlock
import xyz.xenondevs.nova.util.pitch
import xyz.xenondevs.nova.util.yaw
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import java.util.*
import kotlin.jvm.optionals.getOrNull

object ContextParamTypes {
    
    /**
     * The position of a block.
     * 
     * Autofilled by: none
     * 
     * Autofills:
     * - [BLOCK_WORLD]
     * - [BLOCK_DROPS] with and without [TOOL_ITEM_STACK]
     */
    val BLOCK_POS: ContextParamType<BlockPos> =
        ContextParamType.builder<BlockPos>("block_pos")
            .build()
    
    /**
     * The world of a block.
     *
     * Autofilled by:
     * - [BLOCK_POS]
     */
    val BLOCK_WORLD: ContextParamType<World> =
        ContextParamType.builder<World>("block_world")
            .autofilledBy(::BLOCK_POS) { it.world }
            .build()
    
    /**
     * The custom block type.
     *
     * Autofilled by:
     * - [BLOCK_TYPE] if Nova block
     *
     * Autofills:
     * - [BLOCK_STATE_NOVA]
     * - [BLOCK_TYPE]
     */
    val BLOCK_TYPE_NOVA: ContextParamType<NovaBlock> =
        ContextParamType.builder<NovaBlock>("block_type_nova")
            .autofilledBy(::BLOCK_TYPE) { NovaRegistries.BLOCK[it] }
            .autofilledBy(::BLOCK_STATE_NOVA) { it.block }
            .build()
    
    /**
     * The custom block state.
     *
     * Autofilled by:
     * - [BLOCK_TYPE_NOVA]
     * 
     * Autofills:
     * - [BLOCK_TYPE_NOVA]
     */
    val BLOCK_STATE_NOVA: ContextParamType<NovaBlockState> =
        ContextParamType.builder<NovaBlockState>("block_state_nova")
            .autofilledBy(::BLOCK_TYPE_NOVA) { it.defaultBlockState }
            .build()
    
    /**
     * The tile-entity data of a nova tile-entity.
     * 
     * Autofilled by:
     * - [BLOCK_ITEM_STACK] if data is present
     * 
     * Autofills: none
     */
    val TILE_ENTITY_DATA_NOVA: ContextParamType<Compound> =
        ContextParamType.builder<Compound>("tile_entity_data_nova")
            .autofilledBy(::BLOCK_ITEM_STACK) { itemStack ->
                itemStack.novaCompoundOrNull
                    ?.get<Compound>(TileEntity.TILE_ENTITY_DATA_KEY)
                    ?.let { persistentData -> Compound().also { it["persistent"] = persistentData } }
            }.build()
    
    /**
     * The vanilla block type.
     *
     * Autofilled by:
     * - [BLOCK_TYPE] if vanilla block
     *
     * Autofills:
     * - [BLOCK_TYPE]
     */
    val BLOCK_TYPE_VANILLA: ContextParamType<Material> =
        ContextParamType.builder<Material>("block_type_vanilla")
            .require({ it.isBlock }, { "$it is not a block" })
            .autofilledBy(::BLOCK_TYPE) { BuiltInRegistries.BLOCK.getOptional(it).getOrNull()?.bukkitMaterial }
            .build()
    
    // TODO: block state vanilla
    
    /**
     * The block type as id.
     *
     * Autofilled by:
     * - [BLOCK_TYPE_NOVA]
     * - [BLOCK_TYPE_VANILLA]
     * - [BLOCK_ITEM_STACK]
     *
     * Autofills:
     * - [BLOCK_TYPE_NOVA] if Nova block
     * - [BLOCK_TYPE_VANILLA] if vanilla block
     */
    val BLOCK_TYPE: ContextParamType<ResourceLocation> =
        ContextParamType.builder<ResourceLocation>("block_type")
            .autofilledBy(::BLOCK_TYPE_NOVA) { it.id }
            .autofilledBy(::BLOCK_TYPE_VANILLA) { BuiltInRegistries.BLOCK.getKey(it.nmsBlock) }
            .autofilledBy(::BLOCK_ITEM_STACK) { ResourceLocation(ItemUtils.getId(it)) }
            .build()
    
    /**
     * The face of a block that was clicked.
     *
     * Autofilled by:
     * - [SOURCE_PLAYER]
     * 
     * Autofills: none
     */
    val CLICKED_BLOCK_FACE: ContextParamType<BlockFace> =
        ContextParamType.builder<BlockFace>("clicked_block_face")
            .autofilledBy(::SOURCE_PLAYER) { BlockFaceUtils.determineBlockFaceLookingAt(it.eyeLocation) }
            .build()
    
    /**
     * The hand that was used to interact.
     *
     * Autofilled by: none
     * 
     * Autofills:
     * - [BLOCK_ITEM_STACK] with [SOURCE_ENTITY]
     * - [TOOL_ITEM_STACK] with [SOURCE_ENTITY]
     * - [INTERACTION_ITEM_STACK] with [SOURCE_ENTITY]
     */
    val INTERACTION_HAND: ContextParamType<EquipmentSlot> =
        ContextParamType.builder<EquipmentSlot>("interaction_hand")
            .build()
    
    /**
     * The item stack to be placed as a block.
     *
     * Autofilled by:
     * - [SOURCE_ENTITY] and [INTERACTION_HAND]
     * - [BLOCK_TYPE_NOVA] if the block has an item type
     * - [BLOCK_TYPE_VANILLA] if the block has an item type
     *
     * Autofills:
     * - [TILE_ENTITY_DATA_NOVA] if data is present
     * - [BLOCK_TYPE_NOVA] if data is present
     */
    val BLOCK_ITEM_STACK: ContextParamType<ItemStack> =
        ContextParamType.builder<ItemStack>("block_item_stack")
            // TODO: Validate if item stack represents block. This is currently not supported by CustomItemServices.
            .autofilledBy(::SOURCE_ENTITY, ::INTERACTION_HAND) { entity, hand ->
                (entity as? LivingEntity)?.equipment?.getItem(hand)?.takeUnless { it.isEmpty || !it.type.isBlock }
            }
            .autofilledBy(::BLOCK_TYPE_NOVA) { it.item?.createItemStack() }
            .autofilledBy(::BLOCK_TYPE_VANILLA) { if (it.isBlock) ItemStack(it) else null }
            .build()
    
    /**
     * The item stack used as a tool.
     *
     * Autofilled by:
     * - [SOURCE_ENTITY] and [INTERACTION_HAND]
     * 
     * Autofills:
     * - [BLOCK_DROPS] with [BLOCK_POS]
     */
    val TOOL_ITEM_STACK: ContextParamType<ItemStack> =
        ContextParamType.builder<ItemStack>("tool_item_stack")
            .autofilledBy(::SOURCE_ENTITY, ::INTERACTION_HAND) { entity, hand ->
                (entity as? LivingEntity)?.equipment?.getItem(hand)?.takeUnlessEmpty()
            }
            .build()
    
    /**
     * The item stack used to interact with a something.
     *
     * Autofilled by:
     * - [SOURCE_ENTITY] and [INTERACTION_HAND]
     * 
     * Autofills: none
     */
    val INTERACTION_ITEM_STACK: ContextParamType<ItemStack> =
        ContextParamType.builder<ItemStack>("interaction_item_stack")
            .autofilledBy(::SOURCE_ENTITY, ::INTERACTION_HAND) { entity, hand ->
                (entity as? LivingEntity)?.equipment?.getItem(hand)?.takeUnlessEmpty()
            }
            .build()
    
    /**
     * The [UUID] of the source of an action.
     *
     * Autofilled by
     * - [SOURCE_ENTITY]
     * - [SOURCE_TILE_ENTITY]
     * 
     * Autofills: none
     */
    val SOURCE_UUID: ContextParamType<UUID> =
        ContextParamType.builder<UUID>("source_uuid")
            .autofilledBy(::SOURCE_ENTITY) { it.uniqueId }
            .autofilledBy(::SOURCE_TILE_ENTITY) { it.uuid }
            .build()
    
    /**
     * The location of the source of an action.
     *
     * Autofilled by:
     * - [SOURCE_ENTITY]
     * - [SOURCE_TILE_ENTITY]
     * 
     * Autofills:
     * - [SOURCE_WORLD]
     * - [SOURCE_DIRECTION]
     */
    val SOURCE_LOCATION: ContextParamType<Location> =
        ContextParamType.builder<Location>("source_location")
            .autofilledBy(::SOURCE_ENTITY) { it.location }
            .autofilledBy(::SOURCE_TILE_ENTITY) { tileEntity ->
                val pos = tileEntity.pos
                val facing = tileEntity.blockState[DefaultBlockStateProperties.FACING]
                return@autofilledBy if (facing != null) {
                    Location(pos.world, pos.x, pos.y, pos.z, facing.yaw, facing.pitch)
                } else pos.location
            }
            .build()
    
    /**
     * The world of the source of an action.
     *
     * Autofilled by:
     * - [SOURCE_LOCATION]
     * 
     * Autofills: none
     */
    val SOURCE_WORLD: ContextParamType<World> =
        ContextParamType.builder<World>("source_world")
            .autofilledBy(::SOURCE_LOCATION) { it.world }
            .build()
    
    /**
     * The direction that the source of an action is facing.
     *
     * Autofilled by:
     * - [SOURCE_LOCATION]
     * 
     * Autofills: none
     */
    val SOURCE_DIRECTION: ContextParamType<Vector> =
        ContextParamType.builder<Vector>("source_direction")
            .autofilledBy(::SOURCE_LOCATION) { it.direction }
            .build()
    
    /**
     * The player that is the source of an action.
     *
     * Autofilled by:
     * - [SOURCE_ENTITY] if player
     */
    val SOURCE_PLAYER: ContextParamType<Player> =
        ContextParamType.builder<Player>("source_player")
            .autofilledBy(::SOURCE_ENTITY) { it as? Player }
            .build()
    
    /**
     * The entity that is the source of an action.
     *
     * Autofilled by:
     * - [SOURCE_PLAYER]
     * 
     * Autofills:
     * - [SOURCE_UUID]
     * - [SOURCE_LOCATION]
     * - [SOURCE_PLAYER] if player
     * - [BLOCK_ITEM_STACK] with [INTERACTION_HAND]
     * - [TOOL_ITEM_STACK] with [INTERACTION_HAND]
     * - [INTERACTION_ITEM_STACK] with [INTERACTION_HAND]
     */
    val SOURCE_ENTITY: ContextParamType<Entity> =
        ContextParamType.builder<Entity>("source_entity")
            .autofilledBy(::SOURCE_PLAYER) { it }
            .build()
    
    /**
     * The [TileEntity] that is the source of an action.
     * 
     * Autofilled by: none
     * 
     * Autofills:
     * - [SOURCE_UUID]
     * - [SOURCE_LOCATION]
     */
    val SOURCE_TILE_ENTITY: ContextParamType<TileEntity> =
        ContextParamType.builder<TileEntity>("source_tile_entity")
            .build()
    
    /**
     * Whether block drops should be dropped.
     *
     * Autofilled by: 
     * - [BLOCK_POS] with and without [TOOL_ITEM_STACK]
     * 
     * Autofills: none
     */
    val BLOCK_DROPS: DefaultingContextParamType<Boolean> =
        ContextParamType.builder<Boolean>("block_drops")
            .autofilledBy(::BLOCK_POS, ::TOOL_ITEM_STACK) { pos, tool -> ToolUtils.isCorrectToolForDrops(pos.block, tool) }
            .autofilledBy(::BLOCK_POS) { ToolUtils.isCorrectToolForDrops(it.block, null) }
            .build(false)
    
    /**
     * Whether block storage drops should be dropped.
     *
     * Autofilled by:
     * - [SOURCE_PLAYER]
     * 
     * Autofills: none
     */
    val BLOCK_STORAGE_DROPS: DefaultingContextParamType<Boolean> =
        ContextParamType.builder<Boolean>("block_storage_drops")
            .autofilledBy(::SOURCE_PLAYER) { it.gameMode != GameMode.CREATIVE }
            .build(true)
    
    /**
     * Whether block place effects should be played.
     * 
     * Defaults to `true`.
     * 
     * Autofilled by: none
     * 
     * Autofills: none
     */
    val BLOCK_PLACE_EFFECTS: DefaultingContextParamType<Boolean> =
        ContextParamType.builder<Boolean>("block_place_effects")
            .build(true)
    
    /**
     * Whether block break effects should be played.
     * 
     * Defaults to `true`
     * 
     * Autofilled by: none
     * 
     * Autofills: none
     */
    val BLOCK_BREAK_EFFECTS: DefaultingContextParamType<Boolean> =
        ContextParamType.builder<Boolean>("block_break_effects")
            .build(true)
    
    /**
     * Whether tile-entity limits should be bypassed when placing tile-entity blocks.
     *
     * Placed blocks will still be counted.
     * 
     * Defaults to `false`.
     * 
     * Autofilled by: none
     * 
     * Autofills: none
     */
    val BYPASS_TILE_ENTITY_LIMITS: DefaultingContextParamType<Boolean> =
        ContextParamType.builder<Boolean>("bypass_tile_entity_limits")
            .build(false)
    
}