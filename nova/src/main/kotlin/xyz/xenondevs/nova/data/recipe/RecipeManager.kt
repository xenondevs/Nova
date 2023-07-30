package xyz.xenondevs.nova.data.recipe

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Recipe
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.network.ClientboundPlaceGhostRecipePacket
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlaceRecipePacketEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.data.recipe.impl.RepairItemRecipe
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.registry.NovaRegistries.RECIPE_TYPE
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.containsAll
import xyz.xenondevs.nova.util.data.key
import xyz.xenondevs.nova.util.item.novaItem
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

@RequiresOptIn
annotation class HardcodedRecipes

private val ALLOW_RESULT_OVERWRITE by MAIN_CONFIG.entry<Boolean>("debug", "allow_craft_result_overwrite")

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [AddonsInitializer::class, HooksLoader::class, VanillaRecipeTypes::class]
)
object RecipeManager : Listener {
    
    private val INTERNAL_RECIPES: Map<ResourceLocation, (ResourceLocation) -> MojangRecipe<*>> = mapOf(
        ResourceLocation("minecraft", "repair_item") to ::RepairItemRecipe
    )
    
    private val registeredVanillaRecipes = HashMap<ResourceLocation, MojangRecipe<*>>()
    private val customVanillaRecipes = HashMap<ResourceLocation, MojangRecipe<*>>()
    private val _clientsideRecipes = HashMap<ResourceLocation, MojangRecipe<*>>()
    private val _novaRecipes = HashMap<RecipeType<*>, HashMap<ResourceLocation, NovaRecipe>>()
    private val hardcodedRecipes = ArrayList<Any>()
    
    internal val clientsideRecipes: Map<ResourceLocation, MojangRecipe<*>>
        get() = _clientsideRecipes
    val novaRecipes: Map<RecipeType<*>, Map<ResourceLocation, NovaRecipe>>
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
    
    fun <T : NovaRecipe> getRecipe(type: RecipeType<T>, key: NamespacedKey): T? = getRecipe(type, key.resourceLocation)
    
    @Suppress("UNCHECKED_CAST")
    fun <T : NovaRecipe> getRecipe(type: RecipeType<T>, id: ResourceLocation): T? {
        return _novaRecipes[type]?.get(id) as T?
    }
    
    private fun loadRecipes() {
        RecipesLoader.loadRecipes().forEach(::loadRecipe)
        hardcodedRecipes.forEach(::loadRecipe)
    }
    
    private fun loadRecipe(recipe: Any) {
        when (recipe) {
            is BukkitRecipe -> {
                val serversideRecipe = ServersideRecipe.of(recipe)
                serversideRecipe as Recipe<*> // intersection type of Recipe<*> and ServersideRecipe
                
                MINECRAFT_SERVER.recipeManager.addRecipe(serversideRecipe)
                
                val id = serversideRecipe.id
                _clientsideRecipes[id] = serversideRecipe.clientsideCopy()
                registeredVanillaRecipes[id] = serversideRecipe
                customVanillaRecipes[id] = serversideRecipe
            }
            
            is NovaRecipe -> _novaRecipes.getOrPut(recipe.type) { HashMap() }[recipe.id] = recipe
            
            else -> throw UnsupportedOperationException("Unsupported Recipe Type: ${recipe::class.java}")
        }
    }
    
    private fun loadInternalRecipes() {
        val recipeManager = MINECRAFT_SERVER.recipeManager
        INTERNAL_RECIPES.forEach { (id, recipeConstructor) ->
            val recipe = recipeConstructor.invoke(id)
            
            recipeManager.removeRecipe(id)
            recipeManager.addRecipe(recipe)
            
            registeredVanillaRecipes[id] = recipe
        }
    }
    
    internal fun reload() {
        customVanillaRecipes.keys.forEach(MINECRAFT_SERVER.recipeManager::removeRecipe)
        
        customVanillaRecipes.clear()
        _clientsideRecipes.clear()
        _novaRecipes.clear()
        
        loadRecipes()
        RecipeRegistry.indexRecipes()
        RECIPE_TYPE.forEach { it.group.invalidateCache() }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        customVanillaRecipes.keys.forEach { player.discoverRecipe(it.namespacedKey) }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private fun handlePrepareItemCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe ?: return
        
        var requiresContainer = recipe.key.resourceLocation in registeredVanillaRecipes.keys
        if (!requiresContainer && event.inventory.contents.any { it?.novaItem != null }) {
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
        val id = event.recipe
        val recipe = registeredVanillaRecipes[id] ?: return
        
        if (recipe is NovaShapedRecipe) {
            runTask { fillCraftingInventory(event.player, recipe) }
            event.isCancelled = true
        } else if (recipe is NovaShapelessRecipe) {
            runTask { fillCraftingInventory(event.player, recipe) }
            event.isCancelled = true
        }
    }
    
    private fun fillCraftingInventory(player: Player, recipe: NovaShapedRecipe) {
        val craftingInventory = player.openInventory.topInventory as CraftingInventory
        
        // clear previous items
        player.addToInventoryOrDrop(craftingInventory.matrix.filterNotNull())
        craftingInventory.matrix = arrayOfNulls(9)
        
        // check if the player has the required ingredients
        val inventory = player.inventory
        if (inventory.containsAll(recipe.requiredChoices)) {
            // fill inventory
            for (x in 0..<recipe.width) {
                for (y in 0..<recipe.height) {
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
            val packet = ClientboundPlaceGhostRecipePacket(player.serverPlayer.containerMenu.containerId, recipe.id)
            player.send(packet)
        }
    }
    
    private fun fillCraftingInventory(player: Player, recipe: NovaShapelessRecipe) {
        val craftingInventory = player.openInventory.topInventory as CraftingInventory
        
        // clear previous items
        player.addToInventoryOrDrop(craftingInventory.matrix.filterNotNull())
        craftingInventory.matrix = arrayOfNulls(craftingInventory.matrix.size)
        
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
            val packet = ClientboundPlaceGhostRecipePacket(player.serverPlayer.containerMenu.containerId, recipe.id)
            player.send(packet)
        }
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
