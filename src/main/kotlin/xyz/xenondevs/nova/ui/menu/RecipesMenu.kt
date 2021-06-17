package xyz.xenondevs.nova.ui.menu

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.impl.SimplePagedItemsGUI
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.AutoCycleItem
import de.studiocode.invui.item.impl.SimpleItem
import de.studiocode.invui.resourcepack.Icon
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.recipe.ConversionNovaRecipe
import xyz.xenondevs.nova.recipe.RecipeManager

internal enum class CraftingType(
    val typeName: String,
    val itemBuilder: ItemBuilder,
    val recipeList: List<ConversionNovaRecipe>
) {
    
    PULVERIZING(
        "Pulverizing",
        NovaMaterial.PULVERIZER.createBasicItemBuilder().setDisplayName("§fPulverizing"),
        RecipeManager.pulverizerRecipes
    ),
    
    PLATE_PRESSING(
        "Plate Pressing",
        NovaMaterial.IRON_PLATE.createBasicItemBuilder().setDisplayName("§fPlate Pressing"),
        RecipeManager.platePressRecipes
    ),
    
    GEAR_PRESSING(
        "Gear Pressing",
        NovaMaterial.IRON_GEAR.createBasicItemBuilder().setDisplayName("§fGear Pressing"),
        RecipeManager.gearPressRecipes
    )
    
}

private val CRAFTING_RESULTS_GUI_STRUCTURE = Structure("" +
    "b - - - - - - - 2" +
    "| x x x x x x x |" +
    "| x x x x x x x |" +
    "| x x x x x x x |" +
    "3 - - < - > - - 4")
    .addIngredient('b', BackItem { RecipesMenu.open(it) })

private val RECIPE_GUIS: Map<CraftingType, Map<ItemStack, List<RecipeGUI>>> =
    CraftingType.values()
        .associateWith { craftingType ->
            craftingType.recipeList.mapTo(HashSet()) { ItemBuilder(it.resultStack).setAmount(1).build() }
                .associateWith { resultStack ->
                    craftingType.recipeList
                        .filter { it.resultStack.isSimilar(resultStack) }
                        .map(::RecipeGUI)
                }
        }

private val CRAFTING_RESULTS_GUIS: Map<CraftingType, GUI> =
    CraftingType.values().associateWith(::CraftingResultsGUI)

object RecipesMenu {
    
    private val GUI = GUIBuilder(GUIType.NORMAL, 9, 1)
        .setStructure("# 1 # # 2 # # 3 #")
        .addIngredient('1', OpenCraftResultsMenuItem(CraftingType.PULVERIZING))
        .addIngredient('2', OpenCraftResultsMenuItem(CraftingType.PLATE_PRESSING))
        .addIngredient('3', OpenCraftResultsMenuItem(CraftingType.GEAR_PRESSING))
        .build()
    
    fun open(player: Player) {
        SimpleWindow(player, "Crafting Guide", GUI).show()
    }
    
}

internal class CraftingResultsGUI(private val type: CraftingType) :
    SimplePagedItemsGUI(
        9, 5,
        type.recipeList
            .mapTo(HashSet()) { ItemBuilder(it.resultStack).setAmount(1).build() }
            .map { OpenCraftTypeMenuItem(type, it) },
        CRAFTING_RESULTS_GUI_STRUCTURE
    )

internal class RecipeGUI(recipe: ConversionNovaRecipe) : SimpleGUI(7, 1) {
    
    init {
        val structure = Structure("# i # > # o #")
            .addIngredient('i', RecipeInputItem(recipe))
            .addIngredient('o', RecipeOutputItem(recipe))
            .addIngredient('>', SimpleItem(NovaMaterial.PROGRESS_ARROW.createBasicItemBuilder()))
        
        applyStructure(structure)
    }
    
}

internal class OpenCraftResultsMenuItem(private val type: CraftingType) : SimpleItem(type.itemBuilder) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        val gui = CRAFTING_RESULTS_GUIS[type]
        SimpleWindow(player, type.typeName, gui).show()
    }
    
}

internal class OpenCraftTypeMenuItem(private val type: CraftingType, private val result: ItemStack) : SimpleItem(ItemBuilder(result)) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        val gui = GUIBuilder(GUIType.PAGED_GUIs, 9, 3)
            .setStructure("" +
                "b - - - - - - - 2" +
                "| x x x x x x x |" +
                "3 - - < - > - - 4")
            .addIngredient('b', BackItem { SimpleWindow(it, type.typeName, CRAFTING_RESULTS_GUIS[type]).show() })
            .setGUIs(RECIPE_GUIS[type]!![result]!!)
            .build()
        
        SimpleWindow(player, type.typeName, gui).show()
    }
    
}

internal class BackItem(openWindow: (Player) -> Unit) : SimpleItem(
    Icon.ARROW_1_LEFT.itemBuilder.setDisplayName("§7Back"),
    { openWindow(it.player) }
)

internal class RecipeInputItem(recipe: ConversionNovaRecipe) : AutoCycleItem(30, *recipe.inputStacks.map { ItemBuilder(it) }.toTypedArray())

internal class RecipeOutputItem(recipe: ConversionNovaRecipe) : SimpleItem(ItemBuilder(recipe.resultStack))
