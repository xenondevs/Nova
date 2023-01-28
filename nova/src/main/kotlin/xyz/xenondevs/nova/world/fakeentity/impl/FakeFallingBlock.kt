package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.FallingBlockEntity
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.FallingBlockMetadata

class FakeFallingBlock(
	location: Location,
	autoRegister: Boolean,
	beforeSpawn: ((FakeFallingBlock, FallingBlockMetadata) -> Unit)?,
	override val metadata: FallingBlockMetadata
) : FakeEntity<FallingBlockMetadata>(location) {
    
    override val entityType: EntityType<FallingBlockEntity> = EntityType.FALLING_BLOCK
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeFallingBlock, FallingBlockMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, FallingBlockMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}