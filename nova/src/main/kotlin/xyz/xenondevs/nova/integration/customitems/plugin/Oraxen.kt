package xyz.xenondevs.nova.integration.customitems.plugin

import io.th0rgal.oraxen.api.OraxenBlocks
import io.th0rgal.oraxen.api.OraxenItems
import io.th0rgal.oraxen.mechanics.Mechanic
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic
import io.th0rgal.oraxen.utils.drops.Drop
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.recipe.ModelDataTest
import xyz.xenondevs.nova.data.recipe.SingleItemTest
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.integration.customitems.CustomBlockType
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import xyz.xenondevs.nova.integration.customitems.CustomItemType
import xyz.xenondevs.nova.util.item.customModelData
import xyz.xenondevs.nova.util.item.displayName

private val Mechanic.drop: Drop?
    get() = when(this) {
        is BlockMechanic -> drop
        is NoteBlockMechanic -> drop
        is StringBlockMechanic -> drop
        else -> null
    }

internal object Oraxen : CustomItemService {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("Oraxen") != null
    
    override fun removeBlock(block: Block, playSound: Boolean, showParticles: Boolean): Boolean {
        return OraxenBlocks.remove(block.location, null)
    }
    
    override fun breakBlock(block: Block, tool: ItemStack?, playSound: Boolean, showParticles: Boolean): List<ItemStack>? {
        val drop = OraxenBlocks.getOraxenBlock(block.location)?.drop ?: return null
        val drops = ArrayList<ItemStack>()
        if (drop.isToolEnough(tool)) {
            // fixme: Missing API feature
        }
        
        OraxenBlocks.remove(block.location, null)
        return drops
    }
    
    override fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>? {
        // fixme: Missing API feature
        return null
    }
    
    override fun placeBlock(item: ItemStack, location: Location, playSound: Boolean): Boolean {
        val id = getId(item) ?: return false
        OraxenBlocks.place(id, location)
        return true
    }
    
    override fun getItemType(item: ItemStack): CustomItemType? {
        return if (getId(item) != null) CustomItemType.NORMAL else null
    }
    
    override fun getBlockType(block: Block): CustomBlockType? {
        return if (OraxenBlocks.isOraxenBlock(block)) CustomBlockType.NORMAL else null
    }
    
    override fun getItemById(id: String): ItemStack? {
        return OraxenItems.getItemById(id.removePrefix("oraxen:")).build()
    }
    
    override fun getItemTest(id: String): SingleItemTest? {
        return getItemById(id)?.let { ModelDataTest(it.type, intArrayOf(it.customModelData), it) }
    }
    
    override fun getId(item: ItemStack): String? {
        return OraxenItems.getIdByItem(item)?.let { "oraxen:$it" }
    }
    
    override fun getId(block: Block): String? {
        return OraxenBlocks.getOraxenBlock(block.location)?.itemID?.let { "oraxen:$it" }
    }
    
    override fun getName(item: ItemStack, locale: String): String? {
        return if (OraxenItems.getIdByItem(item) != null) item.displayName else null
    }
    
    override fun getName(block: Block, locale: String): String? {
        return getId(block)?.let(::getItemById)?.displayName
    }
    
    override fun hasRecipe(key: NamespacedKey): Boolean {
        return key.namespace == "oraxen"
    }
    
    override fun canBreakBlock(block: Block, tool: ItemStack?): Boolean? {
        return OraxenBlocks.getOraxenBlock(block.location)?.drop?.isToolEnough(tool) ?: return null
    }
    
    override fun getBlockItemModelPaths(): Map<NamespacedId, ResourcePath> {
        return emptyMap()
    }
    
}