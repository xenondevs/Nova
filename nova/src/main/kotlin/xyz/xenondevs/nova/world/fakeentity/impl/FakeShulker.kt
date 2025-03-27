package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Shulker
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ShulkerMetadata

class FakeShulker(
    location: Location,
    autoRegister: Boolean,
    beforeSpawn: ((FakeShulker, ShulkerMetadata) -> Unit)?,
    override val metadata: ShulkerMetadata
) : FakeEntity<ShulkerMetadata>(location) {
    
    override val entityType: EntityType<Shulker> = EntityType.SHULKER
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeShulker, ShulkerMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, ShulkerMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}