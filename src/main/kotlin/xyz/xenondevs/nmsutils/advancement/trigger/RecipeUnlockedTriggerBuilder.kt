package xyz.xenondevs.nmsutils.advancement.trigger

import net.minecraft.advancements.critereon.RecipeUnlockedTrigger
import net.minecraft.resources.ResourceLocation

class RecipeUnlockedTriggerBuilder : TriggerBuilder<RecipeUnlockedTrigger.TriggerInstance>() {
    
    private var id: ResourceLocation? = null
    
    fun recipe(id: ResourceLocation) {
        this.id = id
    }
    
    fun recipe(id: String) {
        this.id = ResourceLocation(id)
    }
    
    override fun build(): RecipeUnlockedTrigger.TriggerInstance {
        checkNotNull(id) { "Recipe id is not set" }
        return RecipeUnlockedTrigger.TriggerInstance(player, id!!)
    }
    
}