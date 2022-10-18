package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemMetadata

class FakeItem(
    location: Location,
    autoRegister: Boolean,
    beforeSpawn: ((FakeItem, ItemMetadata) -> Unit)?,
    override val metadata: ItemMetadata
) : FakeEntity<ItemMetadata>(location) {
    
    override val entityType: EntityType<ItemEntity> = EntityType.ITEM
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeItem, ItemMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, ItemMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}