package xyz.xenondevs.nova.integration.customitems.plugin

import io.th0rgal.oraxen.api.OraxenBlocks
import io.th0rgal.oraxen.api.OraxenItems
import io.th0rgal.oraxen.mechanics.Mechanic
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds
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
import xyz.xenondevs.nova.world.pos

private val Mechanic.drop: Drop?
    get() = when (this) {
        is BlockMechanic -> drop
        is NoteBlockMechanic -> drop
        is StringBlockMechanic -> drop
        else -> null
    }

private val Mechanic.blockSounds: BlockSounds?
    get() = when (this) {
        is BlockMechanic -> blockSounds
        is NoteBlockMechanic -> blockSounds
        is StringBlockMechanic -> blockSounds
        else -> null
    }

internal object Oraxen : CustomItemService {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("Oraxen") != null
    
    override fun removeBlock(block: Block, playSound: Boolean, showParticles: Boolean): Boolean {
        val location = block.location
        val oraxenBlock = OraxenBlocks.getOraxenBlock(location)
        if (oraxenBlock != null) {
            if (playSound) {
                val blockSounds = oraxenBlock.blockSounds
                if (blockSounds != null) {
                    block.pos.playSound(blockSounds.breakSound, blockSounds.breakVolume, blockSounds.breakPitch)
                }
            }
            
            OraxenBlocks.remove(location, null)
            return true
        }
        
        return false
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