package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.world.item.ItemStack
import xyz.xenondevs.nova.world.fakeentity.metadata.MetadataSerializers

class ItemMetadata : EntityMetadata() {
    
    var item by entry(8, MetadataSerializers.ITEM_STACK, ItemStack.EMPTY)
    
}