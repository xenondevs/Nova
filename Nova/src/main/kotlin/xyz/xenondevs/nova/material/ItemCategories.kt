package xyz.xenondevs.nova.material

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.UpdatableFile
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.ui.menu.item.recipes.handleRecipeChoiceItemClick
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.getInt
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.getString
import xyz.xenondevs.nova.util.item.ItemUtils
import java.io.File
import java.io.InputStream

object ItemCategories : Initializable() {
    
    private const val PATH_IN_JAR = "item_categories.json"
    private val CATEGORIES_FILE = File(NOVA.dataFolder, "configs/item_categories.json").apply { parentFile.mkdirs() }
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer)
    
    lateinit var CATEGORIES: List<ItemCategory>
        private set
    lateinit var OBTAINABLE_MATERIALS: Set<ItemNovaMaterial>
        private set
    lateinit var OBTAINABLE_ITEMS: List<CategorizedItem>
        private set
    
    override fun init() {
        LOGGER.info("Loading item categories")
        
        // Unlike recipes, item categories cannot be disabled
        if (!CATEGORIES_FILE.exists()) UpdatableFile.removeStoredHash(CATEGORIES_FILE)
        
        UpdatableFile.load(CATEGORIES_FILE) {
            val defaultCategories = LinkedHashMap<String, Pair<CategoryPriority, JsonObject>>()
            
            // Core categories
            getResourceAsStream(PATH_IN_JAR)?.run { loadCategories(defaultCategories, "nova", this) }
            
            // Addon categories
            AddonManager.loaders.forEach {
                val stream = getResourceAsStream(it.file, PATH_IN_JAR) ?: return@forEach
                loadCategories(defaultCategories, it.description.id, stream)
            }
            
            val categories = defaultCategories.values.sortedBy { it.first }.map { it.second }
            return@load GSON.toJson(categories).byteInputStream()
        }
        
        CATEGORIES = JsonParser.parseReader(CATEGORIES_FILE.reader()).asJsonArray.mapNotNull(ItemCategory::deserialize)
        OBTAINABLE_ITEMS = CATEGORIES.flatMap { it.items }
        OBTAINABLE_MATERIALS = OBTAINABLE_ITEMS.mapNotNullTo(HashSet()) {
            NovaMaterialRegistry.getOrNull(it.id)
                ?: NovaMaterialRegistry.getNonNamespaced(it.id.removePrefix("nova:")).firstOrNull()
        }
    }
    
    private fun loadCategories(categories: MutableMap<String, Pair<CategoryPriority, JsonObject>>, addonId: String, stream: InputStream) {
        val newCategories = JsonParser.parseReader(stream.reader()) as JsonObject
        newCategories.entrySet().forEach { (id, category) ->
            require(category is JsonObject)
            if (id in categories) {
                val items = categories[id]!!.second.getAsJsonArray("items")
                items.addAll(category.getAsJsonArray("items"))
            } else {
                val preferredPosition = category.getInt("priority")
                category.remove("priority")
                categories[id] = CategoryPriority(addonId, preferredPosition) to category
            }
        }
    }
    
}

data class CategoryPriority(val addonId: String, val preferredPosition: Int?) : Comparable<CategoryPriority> {
    
    override fun compareTo(other: CategoryPriority): Int {
        val otherPreferredPosition = other.preferredPosition
        return if (preferredPosition != null && otherPreferredPosition != null) {
            if (preferredPosition > otherPreferredPosition) 1
            else if (preferredPosition < otherPreferredPosition) -1
            else addonId.compareTo(other.addonId)
        } else addonId.compareTo(other.addonId)
    }
    
}

data class ItemCategory(val name: String, val icon: ItemProvider, val items: List<CategorizedItem>) {
    
    companion object {
        fun deserialize(element: JsonElement): ItemCategory? {
            try {
                element as JsonObject
                
                val name = element.getString("name")!!
                val icon = ItemUtils.getItemBuilder(element.getString("icon")!!, true)
                    .setDisplayName(TranslatableComponent(name))
                    .get()
                val items = element.getAsJsonArray("items").map { CategorizedItem(it.asString) }
                
                return ItemCategory(name, ItemWrapper(icon), items)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            return null
        }
    }
    
}

class CategorizedItem(val id: String) : BaseItem() {
    
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