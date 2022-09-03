package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Slime
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.SlimeMetadata

class FakeSlime(
    location: Location,
    autoRegister: Boolean,
    beforeSpawn: ((FakeSlime, SlimeMetadata) -> Unit)?,
    override val metadata: SlimeMetadata
) : FakeEntity<SlimeMetadata>(location) {
    
    override val entityType: EntityType<Slime> = EntityType.SLIME
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeSlime, SlimeMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, SlimeMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}