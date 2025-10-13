package xyz.xenondevs.nova.world.block.behavior

import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.LEAVES_DISTANCE
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.LEAVES_PERSISTENT
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.WATERLOGGED
import xyz.xenondevs.nova.world.format.WorldDataManager
import kotlin.math.min

internal object LeavesBehavior : BlockBehavior {
    
    override fun ticksRandomly(state: NovaBlockState): Boolean {
        return !state.getOrThrow(LEAVES_PERSISTENT) && state.getOrThrow(LEAVES_DISTANCE) == 7
    }
    
    override fun handleRandomTick(pos: BlockPos, state: NovaBlockState) {
        if (!state.getOrThrow(LEAVES_PERSISTENT) && state.getOrThrow(LEAVES_DISTANCE) >= 7) {
            val event = LeavesDecayEvent(pos.block)
            callEvent(event)
            if (event.isCancelled || WorldDataManager.getBlockState(pos) != state)
                return
            
            val ctx = Context.intention(BlockBreak)
                .param(BlockBreak.BLOCK_POS, pos)
                .param(BlockBreak.BLOCK_STATE_NOVA, state)
                .param(BlockBreak.BLOCK_BREAK_EFFECTS, false)
                .build()
            BlockUtils.breakBlockNaturally(ctx)
        }
    }
    
    override fun handleScheduledTick(pos: BlockPos, state: NovaBlockState) {
        val newState = state.with(LEAVES_DISTANCE, calculateDistance(pos))
        if (state == newState)
            return
        
        WorldDataManager.setBlockState(pos, state.with(LEAVES_DISTANCE, calculateDistance(pos)))
        BlockUtils.broadcastBlockUpdate(pos)
    }
    
    override fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> {
        if (!ctx[BlockBreak.BLOCK_DROPS])
            return emptyList()
        
        val nmsState = pos.nmsBlockState
        val params = LootParams.Builder(pos.world.serverLevel)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos.nmsPos))
            .withParameter(LootContextParams.TOOL, ctx[BlockBreak.TOOL_ITEM_STACK].unwrap())
            .withParameter(LootContextParams.BLOCK_STATE, nmsState)
            .create(LootContextParamSets.BLOCK)
        val lootTable = MINECRAFT_SERVER.reloadableRegistries().getLootTable(nmsState.block.lootTable.get())
        return lootTable.getRandomItems(params).map { it.asBukkitMirror() }
    }
    
    override fun updateShape(pos: BlockPos, state: NovaBlockState, neighborPos: BlockPos): NovaBlockState {
        val level = pos.world.serverLevel
        
        if (state.getOrThrow(WATERLOGGED)) {
            level.scheduleTick(pos.nmsPos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        }
        
        val distance = getDistanceAt(neighborPos)
        if (distance != 1 || state.getOrThrow(LEAVES_DISTANCE) != distance) {
            level.scheduleTick(pos.nmsPos, pos.nmsBlockState.block, 1)
        }
        
        return state
    }
    
    fun getDistanceAt(pos: BlockPos): Int? {
        val state = WorldDataManager.getBlockState(pos)
            ?: return if (Tag.LOGS.isTagged(pos.block.type)) 0 else null
        return state[LEAVES_DISTANCE]
    }
    
    fun calculateDistance(pos: BlockPos): Int {
        var distance = 7
        for (face in CUBE_FACES) {
            val neighborPos = pos.advance(face)
            val dist = getDistanceAt(neighborPos)
                ?: continue
            distance = min(distance, dist + 1)
        }
        
        return distance
    }
    
    override fun pickBlockCreative(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): ItemStack? {
        val type = when (state.block) {
            DefaultBlocks.OAK_LEAVES -> Material.OAK_LEAVES
            DefaultBlocks.SPRUCE_LEAVES -> Material.SPRUCE_LEAVES
            DefaultBlocks.BIRCH_LEAVES -> Material.BIRCH_LEAVES
            DefaultBlocks.JUNGLE_LEAVES -> Material.JUNGLE_LEAVES
            DefaultBlocks.ACACIA_LEAVES -> Material.ACACIA_LEAVES
            DefaultBlocks.DARK_OAK_LEAVES -> Material.DARK_OAK_LEAVES
            DefaultBlocks.MANGROVE_LEAVES -> Material.MANGROVE_LEAVES
            DefaultBlocks.CHERRY_LEAVES -> Material.CHERRY_LEAVES
            DefaultBlocks.AZALEA_LEAVES -> Material.AZALEA_LEAVES
            DefaultBlocks.FLOWERING_AZALEA_LEAVES -> Material.FLOWERING_AZALEA_LEAVES
            DefaultBlocks.PALE_OAK_LEAVES -> Material.PALE_OAK_LEAVES
            else -> throw UnsupportedOperationException("Unknown leaves block type: ${state.block}")
        }
        
        return ItemStack.of(type)
    }
    
}