package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.ItemFrame
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemFrameMetadata

class FakeItemFrame(
    location: Location,
    autoRegister: Boolean,
    beforeSpawn: ((FakeItemFrame, ItemFrameMetadata) -> Unit)?,
    override val metadata: ItemFrameMetadata
) : FakeEntity<ItemFrameMetadata>(location) {
    
    override val entityType: EntityType<ItemFrame> = EntityType.ITEM_FRAME
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeItemFrame, ItemFrameMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, ItemFrameMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}