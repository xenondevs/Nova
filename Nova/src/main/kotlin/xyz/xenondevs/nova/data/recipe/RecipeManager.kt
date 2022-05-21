package xyz.xenondevs.nova.data.recipe

import net.minecraft.nbt.CompoundTag
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.*
import org.bukkit.inventory.RecipeChoice.ExactChoice
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.PacketItems
import xyz.xenondevs.nova.network.event.serverbound.PlaceRecipePacketEvent
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.data.copy
import xyz.xenondevs.nova.util.data.key
import xyz.xenondevs.nova.util.item.customModelData
import xyz.xenondevs.nova.util.item.namelessCopyOrSelf
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.item.unhandledTags
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import net.minecraft.world.item.crafting.Recipe as MojangRecipe

interface ItemTest {
    fun test(item: ItemStack): Boolean
}

interface SingleItemTest : ItemTest {
    val example: ItemStack
}

interface MultiItemTest : ItemTest {
    val examples: List<ItemStack>
}

class ModelDataTest(private val type: Material, private val data: IntArray, override val example: ItemStack) : SingleItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return item.type == type && item.customModelData in data
    }
    
}

class NovaIdTest(private val id: String, override val example: ItemStack) : SingleItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return (item.itemMeta?.unhandledTags?.get("nova") as? CompoundTag)?.getString("id") == id
    }
    
}

class NovaNameTest(private val name: String, override val examples: List<ItemStack>) : MultiItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return (item.itemMeta?.unhandledTags?.get("nova") as? CompoundTag)?.getString("id")
            ?.substringAfter(':') == name
    }
    
}

class ComplexTest(override val example: ItemStack) : SingleItemTest {
    
    override fun test(item: ItemStack): Boolean {
        val testStack = item.namelessCopyOrSelf
        return example.isSimilar(testStack)
    }
    
}

class CustomRecipeChoice(private val tests: List<ItemTest>) : ExactChoice(
    tests.flatMap {
        when (it) {
            is SingleItemTest -> listOf(it.example)
            is MultiItemTest -> it.examples
            else -> throw UnsupportedOperationException()
        }
    }
) {
    
    override fun test(item: ItemStack): Boolean {
        return tests.any { it.test(item) }
    }
    
}

private val ALLOW_RESULT_OVERWRITE by configReloadable { DEFAULT_CONFIG.getBoolean("debug.allow_craft_result_overwrite") }

object RecipeManager : Initializable(), Listener {
    
    private val shapedRecipes = HashMap<NamespacedKey, OptimizedShapedRecipe>()
    private val shapelessRecipes = HashMap<NamespacedKey, ShapelessRecipe>()
    private val furnaceRecipes = HashMap<NamespacedKey, FurnaceRecipe>()
    private val vanillaRegisteredRecipeKeys = ArrayList<NamespacedKey>()
    private val _fakeRecipes = HashMap<NamespacedKey, MojangRecipe<*>>()
    private val _novaRecipes = HashMap<RecipeType<*>, HashMap<NamespacedKey, NovaRecipe>>()
    
    val fakeRecipes: Map<NamespacedKey, MojangRecipe<*>>
        get() = _fakeRecipes
    val novaRecipes: Map<RecipeType<*>, Map<NamespacedKey, NovaRecipe>>
        get() = _novaRecipes
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer)
    
    override fun init() {
        LOGGER.info("Loading recipes")
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        loadRecipes()
    }
    
    private fun loadRecipes() {
        RecipesLoader.loadRecipes().forEach { recipe ->
            when (recipe) {
                is Recipe -> {
                    val key = (recipe as Keyed).key
                    
                    when (recipe) {
                        
                        is ShapedRecipe -> {
                            val optimizedRecipe = OptimizedShapedRecipe(recipe)
                            shapedRecipes[key] = optimizedRecipe
                            
                            val nmsRecipe = NovaShapedRecipe(optimizedRecipe)
                            minecraftServer.recipeManager.addRecipe(nmsRecipe)
                            
                            val result = nmsRecipe.resultItem
                            if (PacketItems.isNovaItem(result))
                                _fakeRecipes[key] = nmsRecipe.copy(PacketItems.getFakeItem(result))
                        }
                        
                        is ShapelessRecipe -> {
                            shapelessRecipes[key] = recipe
                            
                            val nmsRecipe = NovaShapelessRecipe(recipe)
                            minecraftServer.recipeManager.addRecipe(nmsRecipe)
                            
                            val result = nmsRecipe.resultItem
                            if (PacketItems.isNovaItem(result))
                                _fakeRecipes[key] = nmsRecipe.copy(PacketItems.getFakeItem(result))
                        }
                        
                        is FurnaceRecipe -> {
                            furnaceRecipes[key] = recipe
                            
                            val nmsRecipe = NovaFurnaceRecipe(recipe)
                            minecraftServer.recipeManager.addRecipe(nmsRecipe)
                            
                            val result = nmsRecipe.resultItem
                            if (PacketItems.isNovaItem(result))
                                _fakeRecipes[key] = nmsRecipe.copy(PacketItems.getFakeItem(result))
                        }
                        
                        is StonecuttingRecipe -> {
                            Bukkit.addRecipe(recipe)
                            val novaMaterial = recipe.result.novaMaterial
                            if (novaMaterial != null)
                                _fakeRecipes[key] = recipe.copy(novaMaterial.clientsideProvider.get().nmsStack)
                        }
                        
                        else -> Bukkit.addRecipe(recipe)
                    }
                    
                    vanillaRegisteredRecipeKeys += key
                }
                
                is NovaRecipe -> _novaRecipes.getOrPut(recipe.type) { HashMap() }[recipe.key] = recipe
                
                else -> throw UnsupportedOperationException("Unsupported Recipe Type: ${recipe::class.java}")
            }
        }
    }
    
    fun reload() {
        vanillaRegisteredRecipeKeys.forEach { minecraftServer.recipeManager.removeRecipe(it.resourceLocation) }
        
        shapedRecipes.clear()
        shapelessRecipes.clear()
        furnaceRecipes.clear()
        vanillaRegisteredRecipeKeys.clear()
        _fakeRecipes.clear()
        _novaRecipes.clear()
        
        loadRecipes()
        RecipeRegistry.init()
        RecipeTypeRegistry.types.forEach { it.group?.invalidateCache() }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : ConversionNovaRecipe> getConversionRecipeFor(type: RecipeType<T>, input: ItemStack): T? {
        return _novaRecipes[type]?.values?.firstOrNull { (it as ConversionNovaRecipe).input.test(input) } as T?
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : NovaRecipe> getRecipe(type: RecipeType<T>, key: NamespacedKey): T? {
        return _novaRecipes[type]?.get(key) as T?
    }
    
    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        vanillaRegisteredRecipeKeys.forEach(event.player::discoverRecipe)
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    fun handlePrepareItemCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe ?: return
        
        var requiresContainer = recipe.key in vanillaRegisteredRecipeKeys
        if (!requiresContainer && event.inventory.contents.any { it.novaMaterial != null }) {
            // prevent non-Nova recipes from using Nova items
            event.inventory.result = ItemStack(Material.AIR)
            requiresContainer = true
        }
        
        if (requiresContainer) {
            // prevent modification of the recipe result by other plugins
            ReflectionRegistry.PREPARE_ITEM_CRAFT_EVENT_MATRIX_FIELD.set(event, NovaCraftingInventory(recipe, event.inventory))
        }
    }
    
    @EventHandler
    fun handleRecipePlace(event: PlaceRecipePacketEvent) {
        val key = NamespacedKey.fromString(event.packet.recipe.toString())
        if (key in shapedRecipes) {
            runTask { fillCraftingInventory(event.player, shapedRecipes[key]!!) }
            event.isCancelled = true
        } else if (key in shapelessRecipes) {
            runTask { fillCraftingInventory(event.player, shapelessRecipes[key]!!) }
            event.isCancelled = true
        }
    }
    
    private fun fillCraftingInventory(player: Player, recipe: OptimizedShapedRecipe) {
        val craftingInventory = player.openInventory.topInventory as CraftingInventory
        
        // clear previous items
        player.addToInventoryOrDrop(craftingInventory.matrix.filterNotNull())
        craftingInventory.matrix = arrayOfNulls(9)
        
        // check if the player has the required ingredients
        val inventory = player.inventory
        if (inventory.containsAll(recipe.requiredChoices)) {
            // fill inventory
            for (slot in 0 until 9) {
                val choice = recipe.choiceMatrix[slot] ?: continue
                
                val item = inventory.takeFirstOccurrence(choice)
                if (item != null) {
                    // Crafting inventory starts at index 1
                    craftingInventory.setItem(slot + 1, item)
                }
            }
            
        } else {
            // send ghost recipe
            val packet = ClientboundPlaceGhostRecipePacket(player.serverPlayer.containerMenu.containerId, recipe.key)
            player.send(packet)
        }
    }
    
    private fun fillCraftingInventory(player: Player, recipe: ShapelessRecipe) {
        val craftingInventory = player.openInventory.topInventory as CraftingInventory
        
        // clear previous items
        player.addToInventoryOrDrop(craftingInventory.matrix.filterNotNull())
        craftingInventory.matrix = arrayOfNulls(9)
        
        // check if the player has the required ingredients
        val inventory = player.inventory
        if (inventory.containsAll(recipe.choiceList)) {
            // fill inventory
            for ((i, choice) in recipe.choiceList.withIndex()) {
                val item = inventory.takeFirstOccurrence(choice)
                if (item != null) {
                    // Crafting inventory starts at index 1
                    craftingInventory.setItem(i + 1, item)
                }
            }
            
        } else {
            // send ghost recipe
            val packet = ClientboundPlaceGhostRecipePacket(player.serverPlayer.containerMenu.containerId, recipe.key.toString())
            player.send(packet)
        }
    }
    
}

/**
 * Optimizes the recipe matching algorithm by already saving an array of recipe choices in the
 * layout of a crafting inventory.
 */
class OptimizedShapedRecipe(val recipe: ShapedRecipe) {
    
    val requiredChoices: List<RecipeChoice>
    val choiceMatrix: Array<RecipeChoice?>
    val key: String
    
    init {
        val flatShape = recipe.shape.joinToString("")
        choiceMatrix = Array(9) { recipe.choiceMap[flatShape[it]] }
        requiredChoices = flatShape.mapNotNull { recipe.choiceMap[it] }
        key = (recipe as Keyed).key.toString()
    }
    
}

/**
 * A crafting inventory that is set to display the new recipe and prevent subsequent
 * changes to the resulting item.
 */
class NovaCraftingInventory(
    val result: Recipe?,
    val inventory: CraftingInventory
) : CraftingInventory by inventory {
    
    override fun getRecipe(): Recipe? {
        return result
    }
    
    override fun setResult(newResult: ItemStack?) {
        if (ALLOW_RESULT_OVERWRITE)
            inventory.result = newResult
    }
    
}
