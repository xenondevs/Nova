package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import org.bukkit.inventory.ItemStack

class ItemFrameMetadata : EntityMetadata() {
    
    var item: ItemStack? by itemStack(8, false)
    var rotation: Int by entry(9, EntityDataSerializers.INT, 0)
    
}