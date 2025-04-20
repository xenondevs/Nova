package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown.useCooldown
import net.kyori.adventure.key.Key
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Creates a factory for [Cooldown] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param cooldown The cooldown, in ticks.
 * Used when `cooldown` is not specified in the item's config, or `null` to require the presence of a config entry.
 *
 * @param group The cooldown group. Falls back to the item's id if not specified.
 * Used when `cooldown_group is not specified in the item's config.
 */
@Suppress("FunctionName")
fun Cooldown(
    cooldown: Int? = null,
    group: Key? = null
) = ItemBehaviorFactory<Cooldown> {
    val cfg = it.config
    Cooldown(
        cfg.entryOrElse(cooldown, "cooldown"),
        cfg.entryOrElse(group ?: it.id, "cooldown_group")
    )
}

/**
 * Adds a use cooldown to items.
 *
 * @param cooldown The cooldown in ticks.
 * @param group The cooldown group.
 */
class Cooldown(
    cooldown: Provider<Int>,
    group: Provider<Key>
) : ItemBehavior {
    
    /**
     * The cooldown in ticks.
     */
    val cooldown: Int by cooldown
    
    /**
     * The cooldown group.
     */
    val group: Key by group
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider { 
        this[DataComponentTypes.USE_COOLDOWN] = combinedProvider(cooldown, group) { cooldown, group ->
            useCooldown(cooldown / 20f).cooldownGroup(group).build()
        }
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Cooldown(cooldown=$cooldown, group=$group)"
    }
    
}