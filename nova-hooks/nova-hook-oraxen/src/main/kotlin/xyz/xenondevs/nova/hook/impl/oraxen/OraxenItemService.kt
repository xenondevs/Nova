package xyz.xenondevs.nova.hook.impl.oraxen

import io.th0rgal.oraxen.api.OraxenBlocks
import io.th0rgal.oraxen.api.OraxenFurniture
import io.th0rgal.oraxen.api.OraxenItems
import io.th0rgal.oraxen.mechanics.Mechanic
import io.th0rgal.oraxen.mechanics.MechanicsManager
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds
import io.th0rgal.oraxen.utils.drops.Drop
import net.kyori.adventure.text.Component
import net.minecraft.resources.ResourceLocation
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Rotation
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.recipe.ModelDataTest
import xyz.xenondevs.nova.data.recipe.SingleItemTest
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.integration.Hook
import xyz.xenondevs.nova.integration.customitems.CustomBlockType
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import xyz.xenondevs.nova.integration.customitems.CustomItemType
import xyz.xenondevs.nova.util.item.customModelData
import xyz.xenondevs.nova.world.pos
import kotlin.random.Random
import kotlin.streams.asSequence

private val Mechanic.drop: Drop?
    get() = when (this) {
        is BlockMechanic -> drop
        is NoteBlockMechanic -> drop
        is StringBlockMechanic -> drop
        is FurnitureMechanic -> drop
        else -> null
    }

private val Mechanic.blockSounds: BlockSounds?
    get() = when (this) {
        is BlockMechanic -> blockSounds
        is NoteBlockMechanic -> blockSounds
        is StringBlockMechanic -> blockSounds
        is FurnitureMechanic -> blockSounds
        else -> null
    }

private val BLOCK_MECHANIC_FACTORIES = listOfNotNull(
    MechanicsManager.getMechanicFactory("block"),
    MechanicsManager.getMechanicFactory("noteblock"),
    MechanicsManager.getMechanicFactory("stringblock"),
    MechanicsManager.getMechanicFactory("furniture"),
)

@Hook(plugins = ["Oraxen"])
internal object OraxenItemService : CustomItemService {
    
    override fun removeBlock(block: Block, breakEffects: Boolean): Boolean {
        val mechanic = removeAndGetMechanic(block)
            ?: return false
        
        if (breakEffects) {
            val blockSounds = mechanic.blockSounds
            if (blockSounds != null) {
                block.pos.playSound(blockSounds.breakSound, SoundCategory.BLOCKS, blockSounds.breakVolume, blockSounds.breakPitch)
            }
        }
        
        return false
    }
    
    private fun removeAndGetMechanic(block: Block): Mechanic? {
        val location = block.location
        
        val oraxenBlock = OraxenBlocks.getOraxenBlock(location)
        if (oraxenBlock != null) {
            OraxenBlocks.remove(location, null)
            return oraxenBlock
        }
        
        val oraxenFurniture = OraxenFurniture.getFurnitureMechanic(block)
        if (oraxenFurniture != null) {
            OraxenFurniture.remove(location, null)
            return oraxenFurniture
        }
        
        return null
    }
    
    override fun getDrops(block: Block, tool: ItemStack?): List<ItemStack>? {
        val drop = getOraxenDrop(block) ?: return null
        
        val loots = drop.takeUnless { it.isToolEnough(tool) }?.loots
            ?: return emptyList()
        
        val drops = ArrayList<ItemStack>()
        loots.forEach { loot ->
            repeat(loot.maxAmount) {
                if (Random.nextInt(loot.probability) == 0)
                    drops.add(loot.itemStack)
            }
        }
        
        return drops
    }
    
    private fun getOraxenDrop(block: Block): Drop? {
        val mechanic = OraxenBlocks.getOraxenBlock(block.location) ?: OraxenFurniture.getFurnitureMechanic(block)
        return mechanic?.drop
    }
    
    override fun placeBlock(item: ItemStack, location: Location, playSound: Boolean): Boolean {
        val id = getId(item) ?: return false
        OraxenBlocks.place(id, location)
        OraxenFurniture.place(location, id, Rotation.NONE, BlockFace.NORTH)
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
        val name = OraxenBlocks.getOraxenBlock(block.location)?.itemID
            ?: OraxenFurniture.getFurnitureMechanic(block)?.itemID
            ?: return null
        
        return "oraxen:$name"
    }
    
    override fun getName(item: ItemStack, locale: String): Component? {
        return if (OraxenItems.getIdByItem(item) != null)
            item.displayName()
        else null
    }
    
    override fun getName(block: Block, locale: String): Component? {
        val item = getId(block)?.let(OraxenItemService::getItemById) ?: return null
        return item.displayName()
    }
    
    override fun hasRecipe(key: NamespacedKey): Boolean {
        return key.namespace == "oraxen"
    }
    
    override fun canBreakBlock(block: Block, tool: ItemStack?): Boolean? {
        return getOraxenDrop(block)?.isToolEnough(tool) ?: return null
    }
    
    override fun getBlockItemModelPaths(): Map<ResourceLocation, ResourcePath> {
        return OraxenItems.entryStream().asSequence()
            .filter { (id, builder) -> id != null && builder.oraxenMeta?.modelName != null }
            .filter { (id, _) -> BLOCK_MECHANIC_FACTORIES.any { it.getMechanic(id) != null } }
            .associateTo(HashMap()) { (name, builder) ->
                val modelName = builder.oraxenMeta.modelName
                
                val id = ResourceLocation("oraxen", name)
                val path = ResourcePath("oraxen_converted", "oraxen/$modelName")
                
                id to path
            }
    }
    
}