package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class ItemDisplayMetadata : DisplayMetadata() {
    
    var itemStack: ItemStack by entry(22, EntityDataSerializers.ITEM_STACK, ItemStack.EMPTY)
    var itemDisplay: ItemDisplayContext by entry(23, EntityDataSerializers.BYTE, ItemDisplayContext.NONE) { it.ordinal.toByte() }
    
}