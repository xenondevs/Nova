package xyz.xenondevs.nova.data.recipe

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.CampfireRecipe
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.RecipeChoice.ExactChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.SmokingRecipe
import org.bukkit.inventory.StonecuttingRecipe
import xyz.xenondevs.nmsutils.network.ClientboundPlaceGhostRecipePacket
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlaceRecipePacketEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.recipe.impl.RepairItemRecipe
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.registry.NovaRegistries.RECIPE_TYPE
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.containsAll
import xyz.xenondevs.nova.util.data.clientsideCopy
import xyz.xenondevs.nova.util.data.key
import xyz.xenondevs.nova.util.item.customModelData
import xyz.xenondevs.nova.util.item.namelessCopyOrSelf
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.item.unhandledTags
import xyz.xenondevs.nova.util.namespacedKey
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener
import xyz.xenondevs.nova.util.resourceLocation
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.takeFirstOccurrence
import net.minecraft.world.item.crafting.Recipe as MojangRecipe
import org.bukkit.inventory.Recipe as BukkitRecipe

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

class TagTest(private val tag: Tag<Material>, override val examples: List<ItemStack> = tag.values.map(::ItemStack)) : MultiItemTest {
    
    override fun test(item: ItemStack): Boolean {
        return tag.isTagged(item.type) && item.customModelData == 0
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

@RequiresOptIn
annotation class HardcodedRecipes

private val ALLOW_RESULT_OVERWRITE by configReloadable { DEFAULT_CONFIG.getBoolean("debug.allow_craft_result_overwrite") }

@InternalInit(
    stage = InitializationStage.POST_WORLD,
    dependsOn = [AddonsInitializer::class, VanillaRecipeTypes::class]
)
object RecipeManager : Listener {
    
    private val INTERNAL_RECIPES: Map<ResourceLocation, (ResourceLocation) -> MojangRecipe<*>> = mapOf(
        ResourceLocation("minecraft", "repair_item") to ::RepairItemRecipe
    )
    
    private val shapedRecipes = HashMap<NamespacedKey, OptimizedShapedRecipe>()
    private val shapelessRecipes = HashMap<NamespacedKey, ShapelessRecipe>()
    private val furnaceRecipes = HashMap<NamespacedKey, FurnaceRecipe>()
    private val blastFurnaceRecipes = HashMap<NamespacedKey, BlastingRecipe>()
    private val smokerRecipes = HashMap<NamespacedKey, SmokingRecipe>()
    private val campfireRecipes = HashMap<NamespacedKey, CampfireRecipe>()
    private val registeredVanillaRecipeKeys = HashSet<NamespacedKey>()
    private val customVanillaRecipeKeys = HashSet<NamespacedKey>()
    private val _clientsideRecipes = HashMap<NamespacedKey, MojangRecipe<*>>()
    private val _novaRecipes = HashMap<RecipeType<*>, HashMap<NamespacedKey, NovaRecipe>>()
    private val hardcodedRecipes = ArrayList<Any>()
    
    internal val clientsideRecipes: Map<NamespacedKey, MojangRecipe<*>>
        get() = _clientsideRecipes
    val novaRecipes: Map<RecipeType<*>, Map<NamespacedKey, NovaRecipe>>
        get() = _novaRecipes
    
    @InitFun
    private fun init() {
        LOGGER.info("Loading recipes")
        registerEvents()
        registerPacketListener()
        loadRecipes()
        loadInternalRecipes()
    }
    
    //<editor-fold desc="hardcoded recipes", defaultstate="collapsed">
    @HardcodedRecipes
    fun registerHardcodedRecipe(recipe: NovaRecipe) {
        hardcodedRecipes += recipe
    }
    
    @HardcodedRecipes
    @JvmName("registerHardcodedRecipes1")
    fun registerHardcodedRecipes(recipes: Iterable<NovaRecipe>) {
        hardcodedRecipes.addAll(recipes)
    }
    
    @HardcodedRecipes
    fun registerHardcodedRecipe(recipe: BukkitRecipe) {
        hardcodedRecipes += recipe
    }
    
    @HardcodedRecipes
    @JvmName("registerHardcodedRecipes2")
    fun registerHardcodedRecipes(recipes: Iterable<BukkitRecipe>) {
        hardcodedRecipes.addAll(recipes)
    }
    //</editor-fold>
    
    @Suppress("UNCHECKED_CAST")
    fun <T : ConversionNovaRecipe> getConversionRecipeFor(type: RecipeType<T>, input: ItemStack): T? {
        return _novaRecipes[type]?.values?.firstOrNull { (it as ConversionNovaRecipe).input.test(input) } as T?
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : NovaRecipe> getRecipe(type: RecipeType<T>, key: NamespacedKey): T? {
        return _novaRecipes[type]?.get(key) as T?
    }
    
    private fun loadRecipes() {
        RecipesLoader.loadRecipes().forEach(::loadRecipe)
        hardcodedRecipes.forEach(::loadRecipe)
    }
    
    private fun loadRecipe(recipe: Any) {
        when (recipe) {
            is BukkitRecipe -> {
                val key = (recipe as Keyed).key
                
                //<editor-fold desc="vanilla recipe registration", defaultstate="collapsed">
                when (recipe) {
                    
                    is ShapedRecipe -> {
                        val optimizedRecipe = OptimizedShapedRecipe(recipe)
                        shapedRecipes[key] = optimizedRecipe
                        
                        val nmsRecipe = NovaShapedRecipe(optimizedRecipe)
                        MINECRAFT_SERVER.recipeManager.addRecipe(nmsRecipe)
                        
                        _clientsideRecipes[key] = nmsRecipe.clientsideCopy()
                    }
                    
                    is ShapelessRecipe -> {
                        shapelessRecipes[key] = recipe
                        
                        val nmsRecipe = NovaShapelessRecipe(recipe)
                        MINECRAFT_SERVER.recipeManager.addRecipe(nmsRecipe)
                        
                        _clientsideRecipes[key] = nmsRecipe.clientsideCopy()
                    }
                    
                    is FurnaceRecipe -> {
                        furnaceRecipes[key] = recipe
                        
                        val nmsRecipe = NovaFurnaceRecipe(recipe)
                        MINECRAFT_SERVER.recipeManager.addRecipe(nmsRecipe)
                        
                        _clientsideRecipes[key] = nmsRecipe.clientsideCopy()
                    }
                    
                    is BlastingRecipe -> {
                        blastFurnaceRecipes[key] = recipe
                        
                        val nmsRecipe = NovaBlastFurnaceRecipe(recipe)
                        MINECRAFT_SERVER.recipeManager.addRecipe(nmsRecipe)
                        
                        _clientsideRecipes[key] = nmsRecipe.clientsideCopy()
                    }
                    
                    is SmokingRecipe -> {
                        smokerRecipes[key] = recipe
                        
                        val nmsRecipe = NovaSmokerRecipe(recipe)
                        MINECRAFT_SERVER.recipeManager.addRecipe(nmsRecipe)
                        
                        _clientsideRecipes[key] = nmsRecipe.clientsideCopy()
                    }
                    
                    is CampfireRecipe -> {
                        campfireRecipes[key] = recipe
                        
                        val nmsRecipe = NovaCampfireRecipe(recipe)
                        MINECRAFT_SERVER.recipeManager.addRecipe(nmsRecipe)
                        
                        _clientsideRecipes[key] = nmsRecipe.clientsideCopy()
                    }
                    
                    is StonecuttingRecipe -> {
                        Bukkit.addRecipe(recipe)
                        
                        // TODO
                        
                        _clientsideRecipes[key] = recipe.clientsideCopy()
                    }
                    
                    else -> Bukkit.addRecipe(recipe)
                }
                //</editor-fold>
                
                registeredVanillaRecipeKeys += key
                customVanillaRecipeKeys += key
            }
            
            is NovaRecipe -> _novaRecipes.getOrPut(recipe.type) { HashMap() }[recipe.key] = recipe
            
            else -> throw UnsupportedOperationException("Unsupported Recipe Type: ${recipe::class.java}")
        }
    }
    
    private fun loadInternalRecipes() {
        val recipeManager = MINECRAFT_SERVER.recipeManager
        INTERNAL_RECIPES.forEach { (resourceLocation, recipeConstructor) ->
            recipeManager.removeRecipe(resourceLocation)
            recipeManager.addRecipe(recipeConstructor(resourceLocation))
            
            registeredVanillaRecipeKeys += resourceLocation.namespacedKey
        }
    }
    
    internal fun reload() {
        customVanillaRecipeKeys.forEach { MINECRAFT_SERVER.recipeManager.removeRecipe(it.resourceLocation) }
        
        shapedRecipes.clear()
        shapelessRecipes.clear()
        furnaceRecipes.clear()
        customVanillaRecipeKeys.clear()
        _clientsideRecipes.clear()
        _novaRecipes.clear()
        
        loadRecipes()
        RecipeRegistry.indexRecipes()
        RECIPE_TYPE.forEach { it.group.invalidateCache() }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        customVanillaRecipeKeys.forEach(event.player::discoverRecipe)
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private fun handlePrepareItemCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe ?: return
        
        var requiresContainer = recipe.key in registeredVanillaRecipeKeys
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
    
    @PacketHandler
    private fun handleRecipePlace(event: ServerboundPlaceRecipePacketEvent) {
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
            for (x in 0 until recipe.width) {
                for (y in 0 until recipe.height) {
                    val choice = recipe.getChoice(x, y) ?: continue
                    
                    val item = inventory.takeFirstOccurrence(choice)
                    if (item != null) {
                        // Crafting inventory starts at index 1
                        craftingInventory.setItem(x + y * 3 + 1, item)
                    }
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
internal class OptimizedShapedRecipe(val recipe: ShapedRecipe) {
    
    val width = recipe.shape[0].length
    val height = recipe.shape.size
    val requiredChoices: List<RecipeChoice>
    val flatChoices: Array<RecipeChoice?>
    val choiceMatrix: Array<Array<RecipeChoice?>>
    val key: String = recipe.key.toString()
    
    init {
        val flatShape = recipe.shape.joinToString("")
        flatChoices = Array(flatShape.length) { recipe.choiceMap[flatShape[it]] }
        requiredChoices = recipe.shape.joinToString("").mapNotNull { recipe.choiceMap[it] }
        choiceMatrix = Array(width) { x -> Array(height) { y -> recipe.choiceMap[recipe.shape[y][x]] } }
    }
    
    fun getChoice(x: Int, y: Int): RecipeChoice? {
        return choiceMatrix.getOrNull(x)?.getOrNull(y)
    }
    
}

/**
 * A crafting inventory that is set to display the new recipe and prevent subsequent
 * changes to the resulting item.
 */
internal class NovaCraftingInventory(
    val result: BukkitRecipe?,
    val inventory: CraftingInventory
) : CraftingInventory by inventory {
    
    override fun getRecipe(): BukkitRecipe? {
        return result
    }
    
    override fun setResult(newResult: ItemStack?) {
        if (ALLOW_RESULT_OVERWRITE)
            inventory.result = newResult
    }
    
}
