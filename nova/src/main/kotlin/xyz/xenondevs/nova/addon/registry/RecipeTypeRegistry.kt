package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.data.recipe.NovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeType
import xyz.xenondevs.nova.data.serialization.json.serializer.RecipeDeserializer
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import kotlin.reflect.KClass

interface RecipeTypeRegistry: AddonGetter {
    
    fun<T: NovaRecipe> recipeType(name: String, recipeClass: KClass<T>, group: RecipeGroup<in T>, deserializer: RecipeDeserializer<T>?): RecipeType<T> {
        val id = ResourceLocation(addon, name)
        val recipeType = RecipeType(id, recipeClass, group, deserializer)
        
        NovaRegistries.RECIPE_TYPE[id] = recipeType
        return recipeType
    }

}