package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * Allows items to store and receive damage.
 */
class Damageable(
    maxDurability: Provider<Int>,
    itemDamageOnAttackEntity: Provider<Int>,
    itemDamageOnBreakBlock: Provider<Int>,
    repairIngredient: Provider<RecipeChoice?>
) : ItemBehavior {
    
    val maxDurability by maxDurability
    val itemDamageOnAttackEntity by itemDamageOnAttackEntity
    val itemDamageOnBreakBlock by itemDamageOnBreakBlock
    val repairIngredient by repairIngredient
    
    constructor(
        maxDurability: Int,
        itemDamageOnAttackEntity: Int,
        itemDamageOnBreakBlock: Int,
        repairIngredient: RecipeChoice? = null
    ) : this(
        provider(maxDurability),
        provider(itemDamageOnAttackEntity),
        provider(itemDamageOnBreakBlock),
        provider(repairIngredient)
    )
    
    override val baseDataComponents = maxDurability.map {
        DataComponentMap.builder()
            .set(DataComponents.MAX_DAMAGE, it)
            .set(DataComponents.DAMAGE, 0)
            .build()
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Damageable(" +
            "damage=${itemStack.unwrap().get(DataComponents.DAMAGE) ?: 0}, " +
            "maxDurability=$maxDurability, " +
            "itemDamageOnAttackEntity=$itemDamageOnAttackEntity, " +
            "itemDamageOnBreakBlock=$itemDamageOnBreakBlock, " +
            "repairIngredient=$repairIngredient" +
            ")"
    }
    
    companion object : ItemBehaviorFactory<Damageable> {
        
        override fun create(item: NovaItem): Damageable {
            val cfg = item.config
            return Damageable(
                cfg.entry<Int>(arrayOf("max_durability"), arrayOf("durability")),
                cfg.optionalEntry<Int>("item_damage_on_attack_entity").orElse(0),
                cfg.optionalEntry<Int>("item_damage_on_break_block").orElse(0),
                cfg.optionalEntry<RecipeChoice>("repair_ingredient")
            )
        }
        
    }
    
}