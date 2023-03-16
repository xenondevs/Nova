package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.item.ItemStack

class ItemFrameMetadata : EntityMetadata() {
    
    var item: ItemStack by entry(8, EntityDataSerializers.ITEM_STACK, ItemStack.EMPTY)
    var rotation: Int by entry(9, EntityDataSerializers.INT, 0)
    
}