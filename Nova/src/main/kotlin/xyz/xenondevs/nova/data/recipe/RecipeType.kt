package xyz.xenondevs.nova.data.recipe

import org.bukkit.inventory.*
import xyz.xenondevs.nova.data.serialization.json.*
import xyz.xenondevs.nova.ui.menu.item.recipes.group.*
import xyz.xenondevs.nova.ui.menu.item.recipes.group.hardcoded.*
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

class RecipeType<T : Any> private constructor(
    val dirName: String?,
    val recipeClass: KClass<T>,
    val group: RecipeGroup?,
    val deserializer: RecipeDeserializer<T>?
) {
    
    init {
        _values.add(this)
    }
    
    companion object {
        
        @Suppress("ObjectPropertyName")
        private val _values = ArrayList<RecipeType<*>>()
        val values: List<RecipeType<*>>
            get() = _values
        
        val SHAPED = RecipeType("shaped", ShapedRecipe::class, TableRecipeGroup, ShapedRecipeDeserializer)
        val SHAPELESS = RecipeType("shapeless", ShapelessRecipe::class, TableRecipeGroup, ShapelessRecipeDeserializer)
        val FURNACE = RecipeType("furnace", FurnaceRecipe::class, SmeltingRecipeGroup, FurnaceRecipeDeserializer)
        val SMITHING = RecipeType("smithing", SmithingRecipe::class, SmithingRecipeGroup, SmithingRecipeDeserializer)
        val STONECUTTER = RecipeType("stonecutter", StonecuttingRecipe::class, StonecutterRecipeGroup, StonecutterRecipeDeserializer)
        val PULVERIZER = RecipeType("pulverizer", PulverizerRecipe::class, PulverizingRecipeGroup, PulverizerRecipeDeserializer)
        val GEAR_PRESS = RecipeType("press/gear", GearPressRecipe::class, PressingRecipeGroup, GearPressRecipeDeserializer)
        val PLATE_PRESS = RecipeType("press/plate", PlatePressRecipe::class, PressingRecipeGroup, PlatePressRecipeDeserializer)
        val FLUID_INFUSER = RecipeType("fluid_infuser", FluidInfuserRecipe::class, FluidInfuserRecipeGroup, FluidInfuserRecipeDeserializer)
        val ELECTRIC_BREWING_STAND = RecipeType("electric_brewing_stand", ElectricBrewingStandRecipe::class, null, ElectricBrewingStandRecipeDeserializer)
        val STAR_COLLECTOR = RecipeType(null, StarCollectorRecipe::class, StarCollectorRecipeGroup, null)
        val COBBLESTONE_GENERATOR = RecipeType(null, CobblestoneGeneratorRecipe::class, CobblestoneGeneratorRecipeGroup, null)
        val FREEZER = RecipeType(null, FreezerRecipe::class, FreezerRecipeGroup, null)
        
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> of(recipe: T): RecipeType<out T> {
            val clazz = recipe::class
            return values.first { clazz == it.recipeClass || clazz.superclasses.contains(it.recipeClass) } as RecipeType<out T>
        }
        
    }
    
}