package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.ArmorStand
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ArmorStandMetadata

class FakeArmorStand(
    location: Location,
    autoRegister: Boolean,
    beforeSpawn: ((FakeArmorStand, ArmorStandMetadata) -> Unit)?,
    override val metadata: ArmorStandMetadata
) : FakeEntity<ArmorStandMetadata>(location) {
    
    override val entityType: EntityType<ArmorStand> = EntityType.ARMOR_STAND
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeArmorStand, ArmorStandMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, ArmorStandMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}