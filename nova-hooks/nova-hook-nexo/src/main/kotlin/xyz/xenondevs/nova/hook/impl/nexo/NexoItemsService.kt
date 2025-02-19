package xyz.xenondevs.nova.hook.impl.nexo

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.utils.drops.Drop
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.Hook
import xyz.xenondevs.nova.integration.customitems.CustomBlockType
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import xyz.xenondevs.nova.integration.customitems.CustomItemType
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.world.item.recipe.SingleItemTest
import kotlin.random.Random

@Hook(plugins = ["Nexo"])
internal object NexoItemsService : CustomItemService {

    // Note: Furniture is intentionally not supported because it is not a block,
    // which causes issues like block placer placing multiple furniture pieces into each other
    
    private val NO_DROP = Drop(mutableListOf(), false, false, "no_drops_please")
    
    override fun removeBlock(block: Block, breakEffects: Boolean): Boolean {
        if (NexoBlocks.isCustomBlock(block)) {
            return NexoBlocks.remove(block.location, overrideDrop = NO_DROP)
        }
        
        return false
    }
    
    override fun placeBlock(item: ItemStack, location: Location, playSound: Boolean): Boolean {
        if (NexoBlocks.isCustomBlock(item)) {
            NexoBlocks.place(NexoItems.idFromItem(item), location)
            return true
        }
        
        return false
    }
    
    override fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>? {
        val breakable = NexoBlocks.customBlockMechanic(block.location)?.breakable 
        if (breakable != null) {
            val drop = breakable.drop
            return drop.loots()
                .filter { loot -> drop.canDrop(tool) && Random.nextDouble() > (1 - loot.probability) }
                .map { loot -> loot.getItem(1) }
        }
        
        return null
    }
    
    override fun getItemType(item: ItemStack): CustomItemType? {
        if (getId(item) != null)
            return CustomItemType.NORMAL
        
        return null
    }
    
    override fun getBlockType(block: Block): CustomBlockType? {
        if (NexoBlocks.isCustomBlock(block))
            return CustomBlockType.NORMAL
        
        return null
    }
    
    override fun getItemById(id: String): ItemStack? {
        return NexoItems.itemFromId(id.removePrefix("nexo:"))?.build()
    }
    
    override fun getItemTest(id: String): SingleItemTest? {
        val example = getItemById(id) ?: return null
        
        return object : SingleItemTest {
            override val example = example
            override fun test(item: ItemStack) = getId(item) == id
        }
    }
    
    override fun getId(item: ItemStack): String? {
        val id = NexoItems.idFromItem(item)
            ?: return null
        return "nexo:$id"
    }
    
    override fun getId(block: Block): String? {
        val id = NexoBlocks.customBlockMechanic(block.location)?.itemID
            ?: return null
        return "nexo:$id"
    }
    
    override fun getName(item: ItemStack, locale: String): Component? {
        return item.effectiveName()
    }
    
    override fun getName(block: Block, locale: String): Component? {
        return NexoItems.itemFromId(getId(block))?.itemName
    }
    
    override fun canBreakBlock(block: Block, tool: ItemStack?): Boolean? {
        val breakable = NexoBlocks.customBlockMechanic(block.location)?.breakable
        return breakable?.drop?.canDrop(tool)
    }
    
    override fun getBlockItemModelPaths(): Map<Key, ResourcePath<ResourceType.Model>> {
        // Missing API
        return emptyMap()
    }
    
    
}