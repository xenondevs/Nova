package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.component.UseCooldown
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.world.item.NovaItem
import java.util.Optional

/**
 * Adds a use cooldown to items.
 * 
 * @param cooldown The cooldown in ticks.
 * @param group The cooldown group.
 */
class Cooldown(
    val cooldown: Provider<Int>,
    val group: Provider<ResourceLocation>
) : ItemBehavior {
    
    /**
     * @param cooldown The cooldown in ticks.
     * @param group The cooldown group.
     */
    constructor(cooldown: Int, group: ResourceLocation) : this(
        provider(cooldown),
        provider(group)
    )
    
    override val baseDataComponents = combinedProvider(
        cooldown, group
    ) { cooldown, group ->
        DataComponentMap.builder()
            .set(DataComponents.USE_COOLDOWN, UseCooldown(cooldown / 20f, Optional.of(group)))
            .build()
    }
    
    /**
     * A factory for creating [Cooldown] behaviors from item configurations.
     *
     * Config options:
     * - `cooldown`: The cooldown in ticks.
     */
    companion object : ItemBehaviorFactory<Cooldown> {
        
        override fun create(item: NovaItem): Cooldown {
            return Cooldown(item.config.entry("cooldown"), provider(item.id))
        }
        
    }
    
}