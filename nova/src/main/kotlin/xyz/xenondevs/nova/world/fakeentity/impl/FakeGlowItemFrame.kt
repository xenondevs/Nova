package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.GlowItemFrame
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemFrameMetadata
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemMetadata

class FakeGlowItemFrame(
	location: Location,
	autoRegister: Boolean,
	beforeSpawn: ((FakeGlowItemFrame, ItemFrameMetadata) -> Unit)?,
	override val metadata: ItemFrameMetadata
) : FakeEntity<ItemFrameMetadata>(location) {
    
    override val entityType: EntityType<GlowItemFrame> = EntityType.GLOW_ITEM_FRAME
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeGlowItemFrame, ItemFrameMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, ItemFrameMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}