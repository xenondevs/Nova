package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.item.ItemStack

class ItemFrameMetadata : EntityMetadata() {
    
    var item: ItemStack by itemStack(8, false, ItemStack.EMPTY)
    var rotation: Int by entry(9, EntityDataSerializers.INT, 0)
    
}