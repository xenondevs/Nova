@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.world.fakeentity.FAKE_ENTITY_DEPRECATION

@Deprecated(FAKE_ENTITY_DEPRECATION)
class ItemFrameMetadata : EntityMetadata() {
    
    var item: ItemStack? by itemStack(8, false)
    var rotation: Int by entry(9, EntityDataSerializers.INT, 0)
    
}