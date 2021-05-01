package xyz.xenondevs.nova.util

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.inventory.ItemStack

fun Block.breakAndTakeDrops(): Collection<ItemStack> {
    val drops = drops
    
    val state = state
    if (state is Chest) {
        drops += state.blockInventory.contents.filterNotNull()
        state.blockInventory.clear()
    } else if (state is Container) {
        drops += state.inventory.contents.filterNotNull()
        state.inventory.clear()
    }
    
    type = Material.AIR
    
    return drops
}