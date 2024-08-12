package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform
import org.bukkit.inventory.ItemStack

class ItemDisplayMetadata : DisplayMetadata() {
    
    var itemStack: ItemStack? by itemStack(23, true)
    var itemDisplay: ItemDisplayTransform by entry(24, EntityDataSerializers.BYTE, ItemDisplayTransform.NONE) { it.ordinal.toByte() }
    
}