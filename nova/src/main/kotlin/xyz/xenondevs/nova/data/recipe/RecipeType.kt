package xyz.xenondevs.nova.data.recipe

import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.CampfireRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.SmithingRecipe
import org.bukkit.inventory.SmokingRecipe
import org.bukkit.inventory.StonecuttingRecipe
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.recipe.RecipeTypeRegistry.register
import xyz.xenondevs.nova.data.serialization.json.BlastingRecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.CampfireRecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.FurnaceRecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.RecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.ShapedRecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.ShapelessRecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.SmithingRecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.SmokingRecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.StonecutterRecipeDeserializer
import xyz.xenondevs.nova.ui.menu.item.recipes.group.BlastingRecipeGroup
import xyz.xenondevs.nova.ui.menu.item.recipes.group.CampfireRecipeGroup
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup
import xyz.xenondevs.nova.ui.menu.item.recipes.group.SmeltingRecipeGroup
import xyz.xenondevs.nova.ui.menu.item.recipes.group.SmithingRecipeGroup
import xyz.xenondevs.nova.ui.menu.item.recipes.group.SmokingRecipeGroup
import xyz.xenondevs.nova.ui.menu.item.recipes.group.StonecutterRecipeGroup
import xyz.xenondevs.nova.ui.menu.item.recipes.group.TableRecipeGroup
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
    
    fun <T : NovaRecipe> register(addon: Addon, dirName: String?, recipeClass: KClass<T>, group: RecipeGroup<in T>?, deserializer: RecipeDeserializer<T>?): RecipeType<T> {
        val name = "${addon.description.id}/$dirName"
        return register(name, recipeClass, group, deserializer)
    }
    
    internal fun <T : Any> register(dirName: String?, recipeClass: KClass<T>, group: RecipeGroup<in T>?, deserializer: RecipeDeserializer<T>?): RecipeType<T> {
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
    val group: RecipeGroup<in T>?,
    val deserializer: RecipeDeserializer<T>?
) {
    
    companion object {
        val SHAPED = register("minecraft/shaped", ShapedRecipe::class, TableRecipeGroup, ShapedRecipeDeserializer)
        val SHAPELESS = register("minecraft/shapeless", ShapelessRecipe::class, TableRecipeGroup, ShapelessRecipeDeserializer)
        val FURNACE = register("minecraft/furnace", FurnaceRecipe::class, SmeltingRecipeGroup, FurnaceRecipeDeserializer)
        val BLAST_FURNACE = register("minecraft/blast_furnace", BlastingRecipe::class, BlastingRecipeGroup, BlastingRecipeDeserializer)
        val SMOKER = register("minecraft/smoker", SmokingRecipe::class, SmokingRecipeGroup, SmokingRecipeDeserializer)
        val CAMPFIRE = register("minecraft/campfire", CampfireRecipe::class, CampfireRecipeGroup, CampfireRecipeDeserializer)
        val STONECUTTER = register("minecraft/stonecutter", StonecuttingRecipe::class, StonecutterRecipeGroup, StonecutterRecipeDeserializer)
        val SMITHING = register("minecraft/smithing", SmithingRecipe::class, SmithingRecipeGroup, SmithingRecipeDeserializer)
        
        internal fun init() = Unit
    }
    
}