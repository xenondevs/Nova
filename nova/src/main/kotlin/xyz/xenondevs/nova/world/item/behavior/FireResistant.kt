package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.DamageResistant.damageResistant
import io.papermc.paper.registry.keys.tags.DamageTypeTagKeys
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Makes items fire-resistant.
 */
object FireResistant : ItemBehavior {
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider {
        this[DataComponentTypes.DAMAGE_RESISTANT] = damageResistant(DamageTypeTagKeys.IS_FIRE)
    }
    
}