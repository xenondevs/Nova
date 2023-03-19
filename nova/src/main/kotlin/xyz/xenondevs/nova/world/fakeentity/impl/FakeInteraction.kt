package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Interaction
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.InteractionMetadata

class FakeInteraction(
    location: Location,
    autoRegister: Boolean,
    beforeSpawn: ((FakeInteraction, InteractionMetadata) -> Unit)?,
    override val metadata: InteractionMetadata
) : FakeEntity<InteractionMetadata>(location) {
    
    override val entityType: EntityType<Interaction> = EntityType.INTERACTION
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeInteraction, InteractionMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, InteractionMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}