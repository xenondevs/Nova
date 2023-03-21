package xyz.xenondevs.nova.material

import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.UpdatableFile
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.ui.menu.item.ItemMenu
import xyz.xenondevs.nova.ui.menu.item.recipes.handleRecipeChoiceItemClick
import xyz.xenondevs.nova.util.data.getConfigurationSectionList
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.item.ItemUtils
import java.io.File
import java.io.InputStream
import java.util.logging.Level

@InternalInit(
    stage = InitializationStage.POST_WORLD,
    dependsOn = [AddonsInitializer::class]
)
internal object ItemCategories {
    
    private const val PATH_IN_JAR = "item_categories.yml"
    private val CATEGORIES_FILE = File(NOVA.dataFolder, "configs/item_categories.yml").apply { parentFile.mkdirs() }
    
    lateinit var CATEGORIES: List<ItemCategory>
        private set
    lateinit var OBTAINABLE_ITEMS: List<CategorizedItem>
        private set
    
    fun init() {
        LOGGER.info("Loading item categories")
        reload()
    }
    
    fun reload() {
        // Clear ItemMenu history to prevent old item categories from showing up
        ItemMenu.clearAllHistory()
        
        // Unlike recipes, item categories cannot be disabled
        if (!CATEGORIES_FILE.exists())
            UpdatableFile.removeStoredHash(CATEGORIES_FILE)
        
        UpdatableFile.load(CATEGORIES_FILE) {
            val defaultCategories = LinkedHashMap<String, Pair<CategoryPriority, ConfigurationSection>>()
            
            // Core categories
            getResourceAsStream(PATH_IN_JAR)?.run { loadCategories(defaultCategories, "nova", this) }
            
            // Addon categories
            AddonManager.loaders.forEach { (id, loader) ->
                val stream = getResourceAsStream(loader.file, PATH_IN_JAR) ?: return@forEach
                loadCategories(defaultCategories, id, stream)
            }
            
            val categories = defaultCategories.values.sortedBy { it.first }.map { it.second }
            val cfg = YamlConfiguration()
            cfg.set("categories", categories)
            
            return@load cfg.saveToString().byteInputStream()
        }
        
        CATEGORIES = YamlConfiguration.loadConfiguration(CATEGORIES_FILE.reader())
            .getConfigurationSectionList("categories")
            .mapNotNull(ItemCategory::deserialize)
        
        OBTAINABLE_ITEMS = CATEGORIES.flatMap { it.items }
    }
    
    private fun loadCategories(categories: MutableMap<String, Pair<CategoryPriority, ConfigurationSection>>, addonId: String, stream: InputStream) {
        val newCategories = YamlConfiguration.loadConfiguration(stream.reader())
        newCategories.getKeys(false).forEach { id ->
            val category = newCategories.getConfigurationSection(id)!!
            if (id in categories) {
                val section = categories[id]!!.second
                val items = section.getStringList("items")
                items += category.getStringList("items")
                section.set("items", items)
            } else {
                val preferredPosition = category.getInt("priority")
                category.set("priority", null)
                categories[id] = CategoryPriority(addonId, preferredPosition) to category
            }
        }
    }
    
}

internal data class CategoryPriority(val addonId: String, val preferredPosition: Int?) : Comparable<CategoryPriority> {
    
    override fun compareTo(other: CategoryPriority): Int {
        val otherPreferredPosition = other.preferredPosition
        return if (preferredPosition != null && otherPreferredPosition != null) {
            if (preferredPosition > otherPreferredPosition) 1
            else if (preferredPosition < otherPreferredPosition) -1
            else addonId.compareTo(other.addonId)
        } else addonId.compareTo(other.addonId)
    }
    
}

internal data class ItemCategory(val name: String, val icon: ItemProvider, val items: List<CategorizedItem>) {
    
    companion object {
        fun deserialize(element: ConfigurationSection): ItemCategory? {
            try {
                val name = element.getString("name")!!
                val icon = ItemUtils.getItemBuilder(element.getString("icon")!!, true)
                    .setDisplayName(Component.translatable(name))
                    .get()
                val items = element.getStringList("items").map(::CategorizedItem)
                
                return ItemCategory(name, ItemWrapper(icon), items)
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "Could not deserialize item category", e)
            }
            
            return null
        }
    }
    
}

internal class CategorizedItem(val id: String) : AbstractItem() {
    
    val localizedName: String
    val itemStack: ItemStack
    val itemWrapper: ItemProvider
    
    init {
        val (itemStack, localizedName) = ItemUtils.getItemAndLocalizedName(id)
        this.localizedName = localizedName
        this.itemStack = itemStack
        this.itemWrapper = ItemWrapper(itemStack)
    }
    
    override fun getItemProvider() = itemWrapper
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        handleRecipeChoiceItemClick(player, clickType, event, this@CategorizedItem.itemWrapper)
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is CategorizedItem && id == other.id
    }
    
}