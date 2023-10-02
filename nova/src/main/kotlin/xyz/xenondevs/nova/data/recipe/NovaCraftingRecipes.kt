package xyz.xenondevs.nova.data.recipe

import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.world.Container
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.BlastingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.item.crafting.SmithingTransformRecipe
import net.minecraft.world.item.crafting.SmokingRecipe
import net.minecraft.world.item.crafting.StonecutterRecipe
import net.minecraft.world.level.Level
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.commons.collections.removeFirstWhere
import xyz.xenondevs.nova.item.logic.PacketItems
import xyz.xenondevs.nova.util.NMSUtils.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.NonNullList
import xyz.xenondevs.nova.util.bukkitCopy
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.data.nmsCategory
import xyz.xenondevs.nova.util.data.toNmsIngredient
import xyz.xenondevs.nova.util.nmsCopy
import org.bukkit.inventory.BlastingRecipe as BukkitBlastingRecipe
import org.bukkit.inventory.CampfireRecipe as BukkitCampfireRecipe
import org.bukkit.inventory.FurnaceRecipe as BukkitFurnaceRecipe
import org.bukkit.inventory.Recipe as BukkitRecipe
import org.bukkit.inventory.ShapedRecipe as BukkitShapedRecipe
import org.bukkit.inventory.ShapelessRecipe as BukkitShapelessRecipe
import org.bukkit.inventory.SmithingTransformRecipe as BukkitSmithingTransformRecipe
import org.bukkit.inventory.SmokingRecipe as BukkitSmokingRecipe
import org.bukkit.inventory.StonecuttingRecipe as BukkitStonecuttingRecipe

private fun Ingredient.clientsideCopy(): Ingredient {
    val items = items.map(ItemStack::clientsideCopy)
    return Ingredient(items.stream().map { Ingredient.ItemValue(it) })
}

private fun ItemStack.clientsideCopy(): ItemStack =
    PacketItems.getClientSideStack(null, this)

internal sealed interface ServersideRecipe<T : Recipe<*>> {
    
    fun clientsideCopy(): T
    
    companion object {
        
        fun of(bukkitRecipe: BukkitRecipe): ServersideRecipe<*> = when (bukkitRecipe) {
            is BukkitShapedRecipe -> NovaShapedRecipe.of(bukkitRecipe)
            is BukkitShapelessRecipe -> NovaShapelessRecipe(bukkitRecipe)
            is BukkitFurnaceRecipe -> NovaFurnaceRecipe(bukkitRecipe)
            is BukkitBlastingRecipe -> NovaBlastFurnaceRecipe(bukkitRecipe)
            is BukkitSmokingRecipe -> NovaSmokerRecipe(bukkitRecipe)
            is BukkitCampfireRecipe -> NovaCampfireRecipe(bukkitRecipe)
            is BukkitStonecuttingRecipe -> NovaStonecutterRecipe(bukkitRecipe)
            is BukkitSmithingTransformRecipe -> NovaSmithingTransformRecipe(bukkitRecipe)
            else -> throw UnsupportedOperationException("Unknown recipe type: ${bukkitRecipe::class.simpleName}")
        }
        
    }
    
}

internal class NovaShapedRecipe private constructor(
    private val bukkitRecipe: BukkitShapedRecipe,
    width: Int,
    height: Int,
    choices: NonNullList<Ingredient>,
    result: ItemStack,
    val flatChoices: Array<RecipeChoice?>,
    val requiredChoices: List<RecipeChoice>,
    val choiceMatrix: Array<Array<RecipeChoice?>>
) : ShapedRecipe(
    "", 
    bukkitRecipe.category.nmsCategory, 
    width, height,
    choices, result
), ServersideRecipe<ShapedRecipe> {
    
    fun getChoice(x: Int, y: Int): RecipeChoice? =
        choiceMatrix.getOrNull(x)?.getOrNull(y)
    
    override fun matches(container: CraftingContainer, world: Level): Boolean {
        // Iterate through all the top-left positions of all possible placements of the recipe shape.
        for (startX in 0..container.width - width) {
            for (startY in 0..container.height - height) {
                // Check if the recipe is valid at that position
                if (matchesAt(container, startX, startY, false)
                    || matchesAt(container, startX, startY, true)) return true
            }
        }
        return false
    }
    
    private fun matchesAt(container: CraftingContainer, x: Int, y: Int, horizontalFlip: Boolean): Boolean {
        for (absX in 0..<container.width)
            for (absY in 0..<container.height) {
                // relX and relY is the position relative to the recipe shape
                val relX = absX - x
                val relY = absY - y
                val item = container.getItem(absX + absY * container.width)
                // If relX and relY are in the shape, it will be the RecipeChoice at that position, or null otherwise
                val choice = if (relX in (0..<width) && relY in (0..<height)) {
                    getChoice(if (horizontalFlip) width - relX - 1 else relX, relY)
                } else null
                // If choice is null, treat it as an air RecipeChoice.
                if (choice == null) {
                    if (!item.isEmpty) return false
                } else if (!choice.test(item.bukkitCopy)) return false
            }
        return true
    }
    
    override fun clientsideCopy(): ShapedRecipe {
        val result = getResultItem(REGISTRY_ACCESS).clientsideCopy()
        val ingredients = NonNullList(ingredients.map(Ingredient::clientsideCopy))
        return ShapedRecipe(group, category(), width, height, ingredients, result)
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitShapedRecipe = bukkitRecipe
    
    companion object {
        
        fun of(recipe: BukkitShapedRecipe): NovaShapedRecipe {
            val width = recipe.shape[0].length
            val height = recipe.shape.size
            
            val flatShape: String = recipe.shape.joinToString("")
            val flatChoices: Array<RecipeChoice?> = Array(flatShape.length) { recipe.choiceMap[flatShape[it]] }
            val requiredChoices: List<RecipeChoice> = recipe.shape.joinToString("").mapNotNull { recipe.choiceMap[it] }
            val choiceMatrix: Array<Array<RecipeChoice?>> = Array(width) { x -> Array(height) { y -> recipe.choiceMap[recipe.shape[y][x]] } }
            
            return NovaShapedRecipe(
                recipe,
                width, height,
                NonNullList(flatChoices.map(RecipeChoice?::toNmsIngredient)),
                recipe.result.nmsCopy,
                flatChoices,
                requiredChoices,
                choiceMatrix
            )
        }
        
    }
    
}

internal class NovaShapelessRecipe(private val bukkitRecipe: BukkitShapelessRecipe) : ShapelessRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.result.nmsCopy,
    NonNullList(bukkitRecipe.choiceList.map(RecipeChoice?::toNmsIngredient))
), ServersideRecipe<ShapelessRecipe> {
    
    val choiceList: List<RecipeChoice> = bukkitRecipe.choiceList
    
    override fun matches(container: CraftingContainer, level: Level): Boolean {
        val choiceList = ArrayList(choiceList)
        
        // loop over all items in the inventory and remove matching choices from the choice list
        // if there is an item stack that does not have a matching choice or the choice list is not empty
        // at the end of the loop, the recipe doesn't match
        return container.contents.filterNot { it.isEmpty }.all { matrixStack ->
            choiceList.removeFirstWhere { it.test(matrixStack.bukkitCopy) }
        } && choiceList.isEmpty()
    }
    
    override fun clientsideCopy(): ShapelessRecipe {
        val result = getResultItem(REGISTRY_ACCESS).clientsideCopy()
        val ingredients = NonNullList(ingredients.map(Ingredient::clientsideCopy))
        return ShapelessRecipe(group, category(), result, ingredients)
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitShapelessRecipe = bukkitRecipe
    
}

internal class NovaFurnaceRecipe(private val bukkitRecipe: BukkitFurnaceRecipe) : SmeltingRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.inputChoice.toNmsIngredient(),
    bukkitRecipe.result.nmsCopy,
    bukkitRecipe.experience,
    bukkitRecipe.cookingTime
), ServersideRecipe<SmeltingRecipe> {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(container: Container, level: Level): Boolean {
        return choice.test(container.getItem(0).bukkitCopy)
    }
    
    override fun clientsideCopy(): SmeltingRecipe {
        val result = getResultItem(REGISTRY_ACCESS).clientsideCopy()
        val ingredient = ingredients.first().clientsideCopy()
        return SmeltingRecipe(group, category(), ingredient, result, experience, cookingTime)
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}

internal class NovaBlastFurnaceRecipe(private val bukkitRecipe: BukkitBlastingRecipe) : BlastingRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.inputChoice.toNmsIngredient(),
    bukkitRecipe.result.nmsCopy,
    bukkitRecipe.experience,
    bukkitRecipe.cookingTime
), ServersideRecipe<BlastingRecipe> {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(container: Container, level: Level): Boolean {
        return choice.test(container.getItem(0).bukkitCopy)
    }
    
    override fun clientsideCopy(): BlastingRecipe {
        val result = getResultItem(REGISTRY_ACCESS).clientsideCopy()
        val ingredient = ingredient.clientsideCopy()
        return BlastingRecipe(group, category(), ingredient, result, experience, cookingTime)
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}

internal class NovaSmokerRecipe(private val bukkitRecipe: BukkitSmokingRecipe) : SmokingRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.inputChoice.toNmsIngredient(),
    bukkitRecipe.result.nmsCopy,
    bukkitRecipe.experience,
    bukkitRecipe.cookingTime
), ServersideRecipe<SmokingRecipe> {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(container: Container, level: Level): Boolean {
        return choice.test(container.getItem(0).bukkitCopy)
    }
    
    override fun clientsideCopy(): SmokingRecipe {
        val result = getResultItem(REGISTRY_ACCESS).clientsideCopy()
        val ingredient = ingredients.first().clientsideCopy()
        return SmokingRecipe(group, category(), ingredient, result, experience, cookingTime)
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}

internal class NovaCampfireRecipe(private val bukkitRecipe: BukkitCampfireRecipe) : CampfireCookingRecipe(
    "",
    bukkitRecipe.category.nmsCategory,
    bukkitRecipe.inputChoice.toNmsIngredient(),
    bukkitRecipe.result.nmsCopy,
    bukkitRecipe.experience,
    bukkitRecipe.cookingTime
), ServersideRecipe<CampfireCookingRecipe> {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(container: Container, level: Level): Boolean {
        return choice.test(container.getItem(0).bukkitCopy)
    }
    
    override fun clientsideCopy(): CampfireCookingRecipe {
        val result = getResultItem(REGISTRY_ACCESS).clientsideCopy()
        val ingredient = ingredients.first().clientsideCopy()
        return CampfireCookingRecipe(group, category(), ingredient, result, experience, cookingTime)
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}

internal class NovaStonecutterRecipe(private val bukkitRecipe: BukkitStonecuttingRecipe) : StonecutterRecipe(
    "",
    bukkitRecipe.inputChoice.toNmsIngredient(),
    bukkitRecipe.result.nmsCopy
), ServersideRecipe<StonecutterRecipe> {
    
    private val choice = bukkitRecipe.inputChoice
    
    override fun matches(container: Container, level: Level): Boolean {
        return choice.test(container.getItem(0).bukkitCopy)
    }
    
    override fun clientsideCopy(): StonecutterRecipe {
        val result = getResultItem(REGISTRY_ACCESS).clientsideCopy()
        val ingredient = ingredients.first().clientsideCopy()
        return StonecutterRecipe(group, ingredient, result)
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}

internal class NovaSmithingTransformRecipe(private val bukkitRecipe: BukkitSmithingTransformRecipe) : SmithingTransformRecipe(
    bukkitRecipe.template.toNmsIngredient(),
    bukkitRecipe.base.toNmsIngredient(),
    bukkitRecipe.addition.toNmsIngredient(),
    bukkitRecipe.result.nmsCopy
), ServersideRecipe<SmithingTransformRecipe> {
    
    private val templateChoice = bukkitRecipe.template
    private val baseChoice = bukkitRecipe.base
    private val additionChoice = bukkitRecipe.addition
    
    override fun matches(container: Container, world: Level): Boolean {
        return templateChoice.test(container.getItem(0).bukkitMirror)
            && baseChoice.test(container.getItem(1).bukkitMirror)
            && additionChoice.test(container.getItem(2).bukkitMirror)
    }
    
    override fun assemble(container: Container, registryAccess: RegistryAccess): ItemStack {
        val result = bukkitRecipe.result.nmsCopy
        val currentTag = container.getItem(1).orCreateTag.copy()
        currentTag.remove("nova") // prevent item id override
        result.orCreateTag.merge(currentTag)
        return result
    }
    
    override fun clientsideCopy(): SmithingTransformRecipe {
        val result = getResultItem(REGISTRY_ACCESS).clientsideCopy()
        val template = templateChoice.toNmsIngredient().clientsideCopy()
        val base = baseChoice.toNmsIngredient().clientsideCopy()
        val addition = additionChoice.toNmsIngredient().clientsideCopy()
        return SmithingTransformRecipe(template, base, addition, result)
    }
    
    override fun toBukkitRecipe(id: NamespacedKey): BukkitRecipe = bukkitRecipe
    
}