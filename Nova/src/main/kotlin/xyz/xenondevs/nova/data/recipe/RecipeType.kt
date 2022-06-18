package xyz.xenondevs.nova.data.recipe

import org.bukkit.inventory.*
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.recipe.RecipeTypeRegistry.register
import xyz.xenondevs.nova.data.serialization.json.*
import xyz.xenondevs.nova.ui.menu.item.recipes.group.*
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

object RecipeTypeRegistry {
    
    @Suppress("ObjectPropertyName")
    private val _types = ArrayList<RecipeType<*>>()
    val types: List<RecipeType<*>>
        get() = _types
    
    init {
        RecipeType.init() // Loads the default recipe types
    }
    
    fun <T : Any> register(addon: Addon, dirName: String?, recipeClass: KClass<T>, group: RecipeGroup?, deserializer: RecipeDeserializer<T>?): RecipeType<T> {
        val name = "${addon.description.id}/$dirName"
        return register(name, recipeClass, group, deserializer)
    }
    
    internal fun <T : Any> register(dirName: String?, recipeClass: KClass<T>, group: RecipeGroup?, deserializer: RecipeDeserializer<T>?): RecipeType<T> {
        val recipeType = RecipeType(dirName, recipeClass, group, deserializer)
        _types += recipeType
        return recipeType
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getType(recipe: T): RecipeType<out T> {
        val clazz = recipe::class
        return types.first { clazz == it.recipeClass || clazz.superclasses.contains(it.recipeClass) } as RecipeType<out T>
    }
    
}

class RecipeType<T : Any> internal constructor(
    val dirName: String?,
    val recipeClass: KClass<T>,
    val group: RecipeGroup?,
    val deserializer: RecipeDeserializer<T>?
) {
    
    companion object {
        val SHAPED = register("minecraft/shaped", ShapedRecipe::class, TableRecipeGroup, ShapedRecipeDeserializer)
        val SHAPELESS = register("minecraft/shapeless", ShapelessRecipe::class, TableRecipeGroup, ShapelessRecipeDeserializer)
        val FURNACE = register("minecraft/furnace", FurnaceRecipe::class, SmeltingRecipeGroup, FurnaceRecipeDeserializer)
        val SMITHING = register("minecraft/smithing", SmithingRecipe::class, SmithingRecipeGroup, SmithingRecipeDeserializer)
        val STONECUTTER = register("minecraft/stonecutter", StonecuttingRecipe::class, StonecutterRecipeGroup, StonecutterRecipeDeserializer)
        
        internal fun init() = Unit
    }
    
}