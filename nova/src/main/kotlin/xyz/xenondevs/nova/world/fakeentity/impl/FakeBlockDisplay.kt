package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.Display.BlockDisplay
import net.minecraft.world.entity.EntityType
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.BlockDisplayMetadata

class FakeBlockDisplay(
    location: Location,
    autoRegister: Boolean,
    beforeSpawn: ((FakeBlockDisplay, BlockDisplayMetadata) -> Unit)?,
    override val metadata: BlockDisplayMetadata
) : FakeEntity<BlockDisplayMetadata>(location) {
    
    override val entityType: EntityType<BlockDisplay> = EntityType.BLOCK_DISPLAY
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeBlockDisplay, BlockDisplayMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, BlockDisplayMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}