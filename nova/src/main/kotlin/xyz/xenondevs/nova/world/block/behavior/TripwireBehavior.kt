package xyz.xenondevs.nova.world.block.behavior

import io.papermc.paper.event.entity.EntityInsideBlockEvent
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.TripWireBlock
import net.minecraft.world.level.block.TripWireHookBlock
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.event.CraftEventFactory
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.enumMapOf
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.nmsDirection
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.POWERED
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.TRIPWIRE_ATTACHED
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.TRIPWIRE_DISARMED
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.TRIPWIRE_EAST
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.TRIPWIRE_NORTH
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.TRIPWIRE_SOUTH
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.TRIPWIRE_WEST
import xyz.xenondevs.nova.world.block.state.property.impl.BooleanProperty
import xyz.xenondevs.nova.world.format.WorldDataManager

private const val RECHECK_DELAY = 10
private const val MAX_TRIPWIRE_LENGTH = 41
private val FACE_PROPERTIES: Map<BlockFace, BooleanProperty> = enumMapOf(
    BlockFace.NORTH to TRIPWIRE_NORTH,
    BlockFace.EAST to TRIPWIRE_EAST,
    BlockFace.SOUTH to TRIPWIRE_SOUTH,
    BlockFace.WEST to TRIPWIRE_WEST
)

// TODO: implement disarming (breaking with shears)
internal object TripwireBehavior : BlockBehavior {
    
    override fun updateShape(pos: BlockPos, state: NovaBlockState, neighborPos: BlockPos): NovaBlockState {
        val property = FACE_PROPERTIES[BlockFaceUtils.determineBlockFaceBetween(pos, neighborPos)]
            ?: return state
        val novaNeighbor = neighborPos.novaBlockState
        val nmsNeighbor = neighborPos.nmsBlockState
        return state.with(property, novaNeighbor?.block == DefaultBlocks.TRIPWIRE || nmsNeighbor == Blocks.TRIPWIRE_HOOK)
    }
    
    @Suppress("UnstableApiUsage")
    override fun handleEntityInside(pos: BlockPos, state: NovaBlockState, entity: Entity) {
        if (EntityInsideBlockEvent(entity, pos.block).callEvent() && !state.getOrThrow(POWERED)) {
            checkPressed(pos, state)
        }
    }
    
    override fun handleScheduledTick(pos: BlockPos, state: NovaBlockState) {
        if (state.getOrThrow(POWERED)) {
            checkPressed(pos, state)
        }
    }
    
    override fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<DefaultContextIntentions.BlockBreak>): List<ItemStack> {
        if (!ctx[DefaultContextParamTypes.BLOCK_DROPS])
            return emptyList()
        
        return listOf(ItemStack.of(Material.STRING))
    }
    
    private fun checkPressed(pos: BlockPos, state: NovaBlockState) {
        val entities = pos.world.getNearbyEntities(pos.block.boundingBox)
        val isPowered = state.getOrThrow(POWERED)
        val shouldBePowered = entities.any { !it.nmsEntity.isIgnoringBlockTriggers }
        
        // bukkit event
        if (isPowered != shouldBePowered && state.getOrThrow(TRIPWIRE_ATTACHED) && entities.isNotEmpty()) {
            val allowed = entities.any { entity ->
                val event = when (entity) {
                    is Player -> CraftEventFactory.callPlayerInteractEvent(entity.serverPlayer, Action.PHYSICAL, pos.nmsPos, null, null, null)
                    else -> EntityInteractEvent(entity, pos.block).also(::callEvent)
                }
                !event.isCancelled
            }
            
            if (!allowed)
                return
        }
        
        if (isPowered != shouldBePowered) {
            val newState = state.with(POWERED, shouldBePowered)
            WorldDataManager.setBlockState(pos, newState)
            BlockUtils.broadcastBlockUpdate(pos)
            updateSource(pos, newState)
        }
        
        if (shouldBePowered) {
            pos.world.serverLevel.scheduleTick(pos.nmsPos, Blocks.TRIPWIRE, RECHECK_DELAY)
        }
    }
    
    private fun updateSource(pos: BlockPos, state: NovaBlockState) {
        for (face in arrayOf(BlockFace.SOUTH, BlockFace.WEST)) {
            for (length in 1..MAX_TRIPWIRE_LENGTH) {
                val posThere = pos.advance(face, length)
                
                val novaStateThere = WorldDataManager.getBlockState(posThere)
                if (novaStateThere?.block == DefaultBlocks.TRIPWIRE)
                    continue
                
                val vanillaStateThere = posThere.nmsBlockState
                if (vanillaStateThere.block == Blocks.TRIPWIRE_HOOK
                    && vanillaStateThere.getValue(TripWireHookBlock.FACING) == face.oppositeFace.nmsDirection
                ) {
                    TripWireHookBlock.calculateState(
                        posThere.world.serverLevel, posThere.nmsPos, vanillaStateThere,
                        false, true, length, vanillaBlockStateOf(state)
                    )
                    break
                }
            }
        }
    }
    
    fun vanillaBlockStateOf(nova: NovaBlockState): BlockState =
        Blocks.TRIPWIRE.defaultBlockState()
            .setValue(TripWireBlock.NORTH, nova.getOrThrow(TRIPWIRE_NORTH))
            .setValue(TripWireBlock.EAST, nova.getOrThrow(TRIPWIRE_EAST))
            .setValue(TripWireBlock.SOUTH, nova.getOrThrow(TRIPWIRE_SOUTH))
            .setValue(TripWireBlock.WEST, nova.getOrThrow(TRIPWIRE_WEST))
            .setValue(TripWireBlock.DISARMED, nova.getOrThrow(TRIPWIRE_DISARMED))
            .setValue(TripWireBlock.ATTACHED, nova.getOrThrow(TRIPWIRE_ATTACHED))
            .setValue(TripWireBlock.POWERED, nova.getOrThrow(POWERED))
    
    override fun pickBlockCreative(pos: BlockPos, state: NovaBlockState, ctx: Context<DefaultContextIntentions.BlockInteract>): ItemStack? {
        return ItemStack.of(Material.STRING)
    }
    
}