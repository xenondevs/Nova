package xyz.xenondevs.nova.integration.customitems

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.Integration
import xyz.xenondevs.nova.integration.customitems.plugin.ItemsAdder
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.runTask
import java.util.concurrent.CountDownLatch

object CustomItemServiceManager {
    
    private val PLUGINS: List<CustomItemService> = listOf(ItemsAdder)
        .filter(Integration::isInstalled)
    
    val READY_LATCH = CountDownLatch(PLUGINS.size)
    
    fun placeItem(item: ItemStack, location: Location): Boolean {
        return PLUGINS.any { it.placeItem(item, location) }
    }
    
    fun breakBlock(block: Block, tool: ItemStack?): List<ItemStack>? {
        return PLUGINS.firstNotNullOfOrNull { it.breakBlock(block, tool) }
    }
    
    fun getItemByName(name: String): ItemStack? {
        return PLUGINS.firstNotNullOfOrNull { it.getItemByName(name) }
    }
    
    fun getNameKey(item: ItemStack): String? {
        return PLUGINS.firstNotNullOfOrNull { it.getNameKey(item) }
    }
    
    fun hasNamespace(namespace: String): Boolean {
        return PLUGINS.any { it.hasNamespace(namespace) }
    }
    
    fun runAfterDataLoad(run: () -> Unit) {
        if (PLUGINS.isNotEmpty()) {
            runAsyncTask {
                READY_LATCH.await()
                runTask(run)
            }
        } else run()
    }
    
}