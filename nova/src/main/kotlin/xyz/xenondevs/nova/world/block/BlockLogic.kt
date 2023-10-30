package xyz.xenondevs.nova.world.block

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockInteract
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState

internal class BlockLogic<T : NovaBlockState>(private val behaviors: List<BlockBehavior<T>>) {
    
    fun handleInteract(state: T, ctx: Context<BlockInteract>): Boolean {
        var actionPerformed = false
        behaviors.forEach { actionPerformed = it.handleInteract(state, ctx) || actionPerformed }
        return actionPerformed
    }
    
    fun handlePlace(state: T, ctx: Context<BlockPlace>) {
        behaviors.forEach { it.handlePlace(state, ctx) }
    }
    
    fun handleBreak(state: T, ctx: Context<BlockBreak>) {
        behaviors.forEach { it.handleBreak(state, ctx) }
    }
    
    fun getDrops(state: T, ctx: Context<BlockBreak>): List<ItemStack> {
        return behaviors.flatMap { it.getDrops(state, ctx) }
    }
    
    fun getExp(state: T, ctx: Context<BlockBreak>): Int {
        return behaviors.sumOf { it.getExp(state, ctx) }
    }
    
}