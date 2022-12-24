package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.world.item.ItemStack
import xyz.xenondevs.nova.world.fakeentity.metadata.MetadataSerializers

class ItemFrameMetadata : EntityMetadata() {
    
    var item: ItemStack by entry(8, MetadataSerializers.ITEM_STACK, ItemStack.EMPTY)
    var rotation: Int by entry(9, MetadataSerializers.VAR_INT, 0)
    
}