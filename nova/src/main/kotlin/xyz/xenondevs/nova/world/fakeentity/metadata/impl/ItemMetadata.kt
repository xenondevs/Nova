package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.world.item.ItemStack

class ItemMetadata : EntityMetadata() {
    
    var item: ItemStack by itemStack(8, true, ItemStack.EMPTY)
    
}