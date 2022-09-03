package xyz.xenondevs.nova.world.fakeentity.impl

import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ArmorStandMetadata

class FakeArmorStand(
    location: Location,
    autoRegister: Boolean,
    beforeSpawn: ((FakeArmorStand, ArmorStandMetadata) -> Unit)?,
    override val metadata: ArmorStandMetadata
) : FakeEntity<ArmorStandMetadata>(location) {
    
    constructor(location: Location, autoSpawn: Boolean = true, beforeSpawn: ((FakeArmorStand, ArmorStandMetadata) -> Unit)? = null) :
        this(location, autoSpawn, beforeSpawn, ArmorStandMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}