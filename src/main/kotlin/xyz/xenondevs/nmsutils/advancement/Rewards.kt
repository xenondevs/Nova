package xyz.xenondevs.nmsutils.advancement

import net.minecraft.advancements.AdvancementRewards
import net.minecraft.commands.CommandFunction
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Recipe
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.util.resourceLocation

class Rewards(
    val recipes: List<String>,
    val loot: List<String>,
    val experience: Int
) {
    
    companion object : Adapter<Rewards, AdvancementRewards> {
        
        override fun toNMS(value: Rewards): AdvancementRewards =
            AdvancementRewards(
                value.experience,
                value.loot.map(String::resourceLocation).toTypedArray(),
                value.recipes.map(String::resourceLocation).toTypedArray(),
                CommandFunction.CacheableFunction.NONE
            )
        
    }
    
    class Builder {
        
        private val recipes = ArrayList<String>()
        private val loot = ArrayList<String>()
        private var experience = 0
        
        fun recipe(id: String) {
            recipes += id
        }
        
        fun recipe(id: NamespacedKey) {
            recipes += id.toString()
        }
        
        fun recipe(recipe: Recipe) {
            recipes += (recipe as Keyed).key.toString()
        }
        
        fun loot(id: String) {
            loot += id
        }
        
        fun experience(experience: Int) {
            this.experience = experience
        }
        
        internal fun build(): Rewards {
            return Rewards(recipes, loot, experience)
        }
        
    }
    
}