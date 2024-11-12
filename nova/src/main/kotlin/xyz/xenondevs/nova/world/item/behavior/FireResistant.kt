package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.item.component.DamageResistant
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.provider

/**
 * Makes items fire-resistant.
 */
object FireResistant : ItemBehavior {
    
    override val baseDataComponents: Provider<DataComponentMap>
        get() = provider(
            DataComponentMap.builder()
                .set(DataComponents.DAMAGE_RESISTANT, DamageResistant(DamageTypeTags.IS_FIRE))
                .build()
        )
    
}