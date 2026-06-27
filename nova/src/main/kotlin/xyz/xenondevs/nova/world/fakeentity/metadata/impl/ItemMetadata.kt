@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.world.fakeentity.FAKE_ENTITY_DEPRECATION

@Deprecated(FAKE_ENTITY_DEPRECATION)
class ItemMetadata : EntityMetadata() {
    
    var item: ItemStack? by itemStack(8, true)
    
}