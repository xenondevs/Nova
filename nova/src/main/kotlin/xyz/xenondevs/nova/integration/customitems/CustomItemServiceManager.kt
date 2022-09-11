package xyz.xenondevs.nova.integration.customitems

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.recipe.SingleItemTest
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.integration.InternalIntegration
import xyz.xenondevs.nova.integration.customitems.plugin.ItemsAdder
import xyz.xenondevs.nova.integration.customitems.plugin.MMOItems
import xyz.xenondevs.nova.integration.customitems.plugin.Oraxen

object CustomItemServiceManager : Initializable() {
    
    internal val PLUGINS: List<CustomItemService> by lazy { listOf(ItemsAdder, Oraxen, MMOItems).filter(InternalIntegration::isInstalled) }
    
    override val initializationStage = InitializationStage.POST_WORLD_ASYNC
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
        PLUGINS.forEach(CustomItemService::awaitLoad)
    }
    
    fun placeBlock(item: ItemStack, location: Location, playSound: Boolean): Boolean {
        return PLUGINS.any { it.placeBlock(item, location, playSound) }
    }
    
    fun removeBlock(block: Block, playSound: Boolean, showParticles: Boolean): Boolean {
        return PLUGINS.any { it.removeBlock(block, playSound, showParticles) }
    }
    
    fun breakBlock(block: Block, tool: ItemStack?, playSound: Boolean, showParticles: Boolean): List<ItemStack>? {
        return PLUGINS.firstNotNullOfOrNull { it.breakBlock(block, tool, playSound, showParticles) }
    }
    
    fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>? {
        return PLUGINS.firstNotNullOfOrNull { it.getDrops(block, tool) }
    }
    
    fun getItemType(item: ItemStack): CustomItemType? {
        return PLUGINS.firstNotNullOfOrNull { it.getItemType(item) }
    }
    
    fun getBlockType(block: Block): CustomBlockType? {
        return PLUGINS.firstNotNullOfOrNull { it.getBlockType(block) }
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
    
}