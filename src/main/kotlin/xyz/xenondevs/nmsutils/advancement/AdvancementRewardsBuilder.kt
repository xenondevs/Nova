package xyz.xenondevs.nmsutils.advancement

import net.minecraft.advancements.AdvancementRewards
import net.minecraft.commands.CommandFunction
import net.minecraft.resources.ResourceLocation
import org.bukkit.Keyed
import org.bukkit.inventory.Recipe
import xyz.xenondevs.nmsutils.internal.util.resourceLocation

@AdvancementDsl
class AdvancementRewardsBuilder {
    
    private val recipes = ArrayList<ResourceLocation>()
    private val loot = ArrayList<ResourceLocation>()
    private var experience = 0
    
    fun recipe(id: ResourceLocation) {
        recipes += id
    }
    
    fun recipe(id: String) {
        recipes += ResourceLocation(id)
    }
    
    fun recipe(recipe: Recipe) {
        recipes += (recipe as Keyed).key.resourceLocation
    }
    
    fun loot(id: ResourceLocation) {
        loot += id
    }
    
    fun loot(id: String) {
        loot += ResourceLocation(id)
    }
    
    fun experience(experience: Int) {
        this.experience = experience
    }
    
    internal fun build(): AdvancementRewards =
        AdvancementRewards(
            experience,
            loot.toTypedArray(), recipes.toTypedArray(),
            CommandFunction.CacheableFunction.NONE
        )
    
}