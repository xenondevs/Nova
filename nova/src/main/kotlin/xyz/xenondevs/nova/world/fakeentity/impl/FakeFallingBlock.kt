package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.FallingBlockEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.FallingBlock
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.FallingBlockMetadata
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemFrameMetadata
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemMetadata

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