package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.EntityType
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemDisplayMetadata

class FakeItemDisplay(
    location: Location,
    autoRegister: Boolean,
    beforeSpawn: ((FakeItemDisplay, ItemDisplayMetadata) -> Unit)?,
    override val metadata: ItemDisplayMetadata
) : FakeEntity<ItemDisplayMetadata>(location) {
    
    override val entityType: EntityType<ItemDisplay> = EntityType.ITEM_DISPLAY
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeItemDisplay, ItemDisplayMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, ItemDisplayMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}