package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.serialization.json.serializer.RecipeDeserializer
import xyz.xenondevs.nova.ui.menu.explorer.recipes.group.RecipeGroup
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.item.recipe.NovaRecipe
import xyz.xenondevs.nova.world.item.recipe.RecipeType
import kotlin.reflect.KClass

interface RecipeTypeRegistry : AddonHolder {
    
    fun <T : NovaRecipe> registerRecipeType(name: String, recipeClass: KClass<T>, group: RecipeGroup<in T>, deserializer: RecipeDeserializer<T>?): RecipeType<T> {
        val id = Key(addon, name)
        val recipeType = RecipeType(id, recipeClass, group, deserializer)
        
        NovaRegistries.RECIPE_TYPE[id] = recipeType
        return recipeType
    }
    
}