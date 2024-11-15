package xyz.xenondevs.nova.world.item.behavior

import net.kyori.adventure.key.Key
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.UseCooldown
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.util.toKey
import xyz.xenondevs.nova.util.toResourceLocation
import java.util.Optional

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
        cfg.entryOrElse(group ?: it.id.toKey(), "cooldown_group")
    )
}

/**
 * Adds a use cooldown to items.
 *
 * @param cooldown The cooldown in ticks.
 * @param group The cooldown group.
 */
class Cooldown(
    val cooldown: Provider<Int>,
    val group: Provider<Key>
) : ItemBehavior {
    
    override val baseDataComponents = combinedProvider(
        cooldown, group
    ) { cooldown, group ->
        DataComponentMap.builder()
            .set(DataComponents.USE_COOLDOWN, UseCooldown(cooldown / 20f, Optional.of(group.toResourceLocation())))
            .build()
    }
    
}