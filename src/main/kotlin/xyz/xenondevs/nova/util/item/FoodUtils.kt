package xyz.xenondevs.nova.util.item

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Animals
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Tameable
import xyz.xenondevs.nova.util.enumMapOf

val LivingEntity.genericMaxHealth: Double
    get() = getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value

@Suppress("DEPRECATION")
val Animals.canBredNow: Boolean
    get() = !isLoveMode && canBreed() && if (this is Tameable) this.isTamed else true

object FoodUtils {
    
    private val HEALING_TYPES: Map<EntityType, Map<Material, Double>>
    private val BREEDING_TYPES: Map<EntityType, List<Material>>
    private val ITEM_REMAINS: Map<Material, Material>
    private val FOOD_TYPES: Set<Material>
    
    init {
        // --- healing foods --
        val horseHealFoods = mapOf(
            Material.SUGAR to 1.0,
            Material.WHEAT to 2.0,
            Material.APPLE to 3.0,
            Material.GOLDEN_CARROT to 4.0,
            Material.GOLDEN_APPLE to 10.0,
            Material.HAY_BLOCK to 20.0
        )
        
        val dogHealFoods = mapOf(
            Material.MUTTON to 2.0,
            Material.CHICKEN to 1.0,
            Material.PORKCHOP to 2.0,
            Material.BEEF to 2.0,
            Material.RABBIT to 2.0,
            Material.ROTTEN_FLESH to 2.0,
            Material.COOKED_CHICKEN to 1.0,
            Material.COOKED_RABBIT to 1.0,
            Material.COOKED_MUTTON to 2.0,
            Material.COOKED_PORKCHOP to 2.0,
            Material.COOKED_BEEF to 2.0
        )
        
        val catHealFoods = mapOf(
            Material.COD to 2.0,
            Material.SALMON to 2.0
        )
        
        HEALING_TYPES = enumMapOf(
            EntityType.HORSE to horseHealFoods,
            EntityType.DONKEY to horseHealFoods,
            EntityType.MULE to horseHealFoods,
            EntityType.WOLF to dogHealFoods,
            EntityType.CAT to catHealFoods,
            
            EntityType.LLAMA to mapOf(
                Material.WHEAT to 2.0,
                Material.HAY_BLOCK to 10.0
            )
        )
        
        // -- breeding foods --
        val horseBreedFoods = listOf(
            Material.GOLDEN_APPLE,
            Material.ENCHANTED_GOLDEN_APPLE,
            Material.GOLDEN_CARROT
        )
        
        val catBreedFoods = catHealFoods.map { it.key }
        val wheat = listOf(Material.WHEAT)
        
        BREEDING_TYPES = enumMapOf(
            EntityType.LLAMA to listOf(Material.HAY_BLOCK),
            EntityType.TURTLE to listOf(Material.SEAGRASS),
            EntityType.PANDA to listOf(Material.BAMBOO),
            EntityType.FOX to listOf(Material.SWEET_BERRIES),
            EntityType.BEE to Tag.FLOWERS.values.toList(),
            EntityType.STRIDER to listOf(Material.WARPED_FUNGUS),
            EntityType.HOGLIN to listOf(Material.CRIMSON_FUNGUS),
            EntityType.WOLF to dogHealFoods.map { it.key },
            EntityType.CAT to catBreedFoods,
            EntityType.OCELOT to catBreedFoods,
            EntityType.HORSE to horseBreedFoods,
            EntityType.DONKEY to horseBreedFoods,
            EntityType.COW to wheat,
            EntityType.MUSHROOM_COW to wheat,
            EntityType.SHEEP to wheat,
            EntityType.GOAT to wheat,
            EntityType.PIG to listOf(
                Material.CARROT,
                Material.POTATO,
                Material.BEETROOT
            ),
            EntityType.CHICKEN to listOf(
                Material.WHEAT_SEEDS,
                Material.PUMPKIN_SEEDS,
                Material.MELON_SEEDS,
                Material.BEETROOT_SEEDS
            ),
            EntityType.RABBIT to listOf(
                Material.DANDELION,
                Material.CARROT,
                Material.GOLDEN_CARROT
            ),
            EntityType.AXOLOTL to listOf(
                Material.TROPICAL_FISH,
                Material.TROPICAL_FISH_BUCKET
            )
        )
        
        val foodTypes = HashSet<Material>()
        foodTypes += HEALING_TYPES.flatMap { it.value.toList() }.map { it.first }
        foodTypes += BREEDING_TYPES.flatMap { it.value }
        FOOD_TYPES = foodTypes
        
        ITEM_REMAINS = mapOf(Material.TROPICAL_FISH_BUCKET to Material.BUCKET)
    }
    
    fun requiresHealing(entity: LivingEntity): Boolean =
        entity.health < entity.genericMaxHealth && HEALING_TYPES.containsKey(entity.type)
    
    fun getHealAmount(entity: LivingEntity, material: Material): Double =
        HEALING_TYPES[entity.type]?.get(material) ?: 0.0
    
    fun canUseBreedFood(entity: LivingEntity, material: Material): Boolean =
        BREEDING_TYPES[entity.type]?.contains(material) ?: false
    
    fun isFood(material: Material): Boolean =
        FOOD_TYPES.contains(material)
    
    fun getItemRemains(material: Material) =
        ITEM_REMAINS[material]
    
}