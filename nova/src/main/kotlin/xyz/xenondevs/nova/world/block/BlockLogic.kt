package xyz.xenondevs.nova.world.block

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockInteractContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

internal class BlockLogic<T : NovaBlockState>(private val behaviors: List<BlockBehavior<T>>) {
    
    fun handleInteract(state: T, ctx: BlockInteractContext): Boolean {
        var actionPerformed = false
        behaviors.forEach { actionPerformed = it.handleInteract(state, ctx) || actionPerformed }
        return actionPerformed
    }
    
    fun handlePlace(state: T, ctx: BlockPlaceContext) {
        behaviors.forEach { it.handlePlace(state, ctx) }
    }
    
    fun handleBreak(state: T, ctx: BlockBreakContext) {
        behaviors.forEach { it.handleBreak(state, ctx) }
    }
    
    fun getDrops(state: T, ctx: BlockBreakContext): List<ItemStack> {
        return behaviors.flatMap { it.getDrops(state, ctx) }
    }
    
    fun getExp(state: T, ctx: BlockBreakContext): Int {
        return behaviors.sumOf { it.getExp(state, ctx) }
    }
    
}