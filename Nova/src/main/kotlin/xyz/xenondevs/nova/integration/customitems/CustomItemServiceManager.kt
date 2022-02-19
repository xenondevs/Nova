package xyz.xenondevs.nova.integration.customitems

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.recipe.SingleItemTest
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.Integration
import xyz.xenondevs.nova.integration.customitems.plugin.ItemsAdder
import xyz.xenondevs.nova.integration.customitems.plugin.MMOItems
import xyz.xenondevs.nova.integration.customitems.plugin.Oraxen
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.runTask
import java.util.concurrent.CountDownLatch

object CustomItemServiceManager : Initializable() {
    
    private val PLUGINS: List<CustomItemService> = listOf(ItemsAdder, Oraxen, MMOItems)
        .filter(Integration::isInstalled)
    
    private val LOAD_DELAYING_PLUGINS_AMOUNT = PLUGINS.count(CustomItemService::requiresLoadDelay)
    val READY_LATCH = CountDownLatch(LOAD_DELAYING_PLUGINS_AMOUNT)
    
    override val inMainThread = false
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
        READY_LATCH.await()
    }
    
    fun placeItem(item: ItemStack, location: Location, playEffects: Boolean): Boolean {
        return PLUGINS.any { it.placeBlock(item, location, playEffects) }
    }
    
    fun removeBlock(block: Block, playEffects: Boolean): Boolean {
        return PLUGINS.any { it.removeBlock(block, playEffects) }
    }
    
    fun breakBlock(block: Block, tool: ItemStack?, playEffects: Boolean): List<ItemStack>? {
        return PLUGINS.firstNotNullOfOrNull { it.breakBlock(block, tool, playEffects) }
    }
    
    fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>? {
        return PLUGINS.firstNotNullOfOrNull { it.getDrops(block, tool) }
    }
    
    fun getItemByName(name: String): ItemStack? {
        return PLUGINS.firstNotNullOfOrNull { it.getItemByName(name) }
    }
    
    fun getItemTest(name: String): SingleItemTest? {
        return PLUGINS.firstNotNullOfOrNull { it.getItemTest(name) }
    }
    
    fun getNameKey(item: ItemStack): String? {
        return PLUGINS.firstNotNullOfOrNull { it.getId(item) }
    }
    
    fun hasRecipe(key: NamespacedKey): Boolean {
        return PLUGINS.any { it.hasRecipe(key) }
    }
    
    fun runAfterDataLoad(run: () -> Unit) {
        if (LOAD_DELAYING_PLUGINS_AMOUNT != 0) {
            runAsyncTask {
                READY_LATCH.await()
                runTask(run)
            }
        } else run()
    }
    
}