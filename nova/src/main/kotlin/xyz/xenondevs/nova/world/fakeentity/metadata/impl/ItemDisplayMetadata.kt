package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class ItemDisplayMetadata : DisplayMetadata() {
    
    var itemStack: ItemStack by entry(22, EntityDataSerializers.ITEM_STACK, ItemStack.EMPTY)
    private var _itemDisplay: Byte by entry(23, EntityDataSerializers.BYTE, 0)
    var itemDisplay: ItemDisplayContext
        get() = ItemDisplayContext.values()[_itemDisplay.toInt()]
        set(value) {
            _itemDisplay = value.ordinal.toByte()
        }
    
}