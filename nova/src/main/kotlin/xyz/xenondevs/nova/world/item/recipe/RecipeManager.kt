package xyz.xenondevs.nova.world.item.recipe

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.RecipeHolder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.registry.NovaRegistries.RECIPE_TYPE
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.data.key
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.namespacedKey
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.resourceLocation
import net.minecraft.world.item.crafting.Recipe as MojangRecipe
import org.bukkit.inventory.BlastingRecipe as BukkitBlastingRecipe
import org.bukkit.inventory.CampfireRecipe as BukkitCampfireRecipe
import org.bukkit.inventory.FurnaceRecipe as BukkitFurnaceRecipe
import org.bukkit.inventory.Recipe as BukkitRecipe
import org.bukkit.inventory.ShapedRecipe as BukkitShapedRecipe
import org.bukkit.inventory.ShapelessRecipe as BukkitShapelessRecipe
import org.bukkit.inventory.SmithingTransformRecipe as BukkitSmithingTransformRecipe
import org.bukkit.inventory.SmokingRecipe as BukkitSmokingRecipe
import org.bukkit.inventory.StonecuttingRecipe as BukkitStonecuttingRecipe

@RequiresOptIn
annotation class HardcodedRecipes

private val ALLOW_RESULT_OVERWRITE by MAIN_CONFIG.entry<Boolean>("debug", "allow_craft_result_overwrite")
private val ALLOWED_RECIPES = setOf(NamespacedKey("minecraft", "repair_item"), NamespacedKey("minecraft", "armor_dye"))

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [HooksLoader::class, VanillaRecipeTypes::class]
)
object RecipeManager : Listener, PacketListener {
    
    private val registeredVanillaRecipes = HashMap<ResourceKey<MojangRecipe<*>>, RecipeHolder<*>>()
    private val customVanillaRecipes = HashMap<ResourceKey<MojangRecipe<*>>, MojangRecipe<*>>()
    private val _novaRecipes = HashMap<RecipeType<*>, HashMap<ResourceLocation, NovaRecipe>>()
    private val hardcodedRecipes = ArrayList<Any>()
    
    val novaRecipes: Map<RecipeType<*>, Map<ResourceLocation, NovaRecipe>>
        get() = _novaRecipes
    
    @InitFun
    private fun init() {
        registerEvents()
        registerPacketListener()
        loadRecipes()
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
        RecipesLoader.extractAndLoadRecipes().forEach(RecipeManager::loadRecipe)
        hardcodedRecipes.forEach(RecipeManager::loadRecipe)
    }
    
    private fun loadRecipe(recipe: Any) {
        when (recipe) {
            is BukkitRecipe -> {
                val nmsRecipe = when (recipe) {
                    is BukkitShapedRecipe -> NovaShapedRecipe.of(recipe)
                    is BukkitShapelessRecipe -> NovaShapelessRecipe(recipe)
                    is BukkitFurnaceRecipe -> NovaFurnaceRecipe(recipe)
                    is BukkitBlastingRecipe -> NovaBlastFurnaceRecipe(recipe)
                    is BukkitSmokingRecipe -> NovaSmokerRecipe(recipe)
                    is BukkitCampfireRecipe -> NovaCampfireRecipe(recipe)
                    is BukkitStonecuttingRecipe -> NovaStonecutterRecipe(recipe)
                    is BukkitSmithingTransformRecipe -> NovaSmithingTransformRecipe(recipe)
                    else -> throw UnsupportedOperationException("Unknown recipe type: ${recipe::class.simpleName}")
                }
                
                val key = ResourceKey.create(Registries.RECIPE, recipe.key.resourceLocation)
                val holder = RecipeHolder(key, nmsRecipe)
                MINECRAFT_SERVER.recipeManager.addRecipe(holder)
                
                registeredVanillaRecipes[key] = holder
                customVanillaRecipes[key] = nmsRecipe
            }
            
            is NovaRecipe -> _novaRecipes.getOrPut(recipe.type) { HashMap() }[recipe.id] = recipe
            
            else -> throw UnsupportedOperationException("Unsupported Recipe Type: ${recipe::class.java}")
        }
    }
    
    internal fun reload() {
        for (key in customVanillaRecipes.keys) {
            MINECRAFT_SERVER.recipeManager.removeRecipe(key)
        }
        
        customVanillaRecipes.clear()
        _novaRecipes.clear()
        
        loadRecipes()
        RecipeRegistry.indexRecipes()
        RECIPE_TYPE.forEach { it.group.invalidateCache() }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        customVanillaRecipes.keys.forEach { player.discoverRecipe(it.location().namespacedKey) }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private fun handlePrepareItemCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe ?: return
        
        if (recipe.key in ALLOWED_RECIPES)
            return
        
        var requiresContainer = ResourceKey.create(Registries.RECIPE, recipe.key.resourceLocation) in registeredVanillaRecipes.keys
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
