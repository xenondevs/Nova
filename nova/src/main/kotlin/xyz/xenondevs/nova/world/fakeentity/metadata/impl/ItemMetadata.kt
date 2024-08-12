package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import org.bukkit.inventory.ItemStack


class ItemMetadata : EntityMetadata() {
    
    var item: ItemStack? by itemStack(8, true)
    
}