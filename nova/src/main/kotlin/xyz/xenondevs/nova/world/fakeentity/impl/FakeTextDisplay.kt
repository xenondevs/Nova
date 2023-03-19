package xyz.xenondevs.nova.world.fakeentity.impl

import net.minecraft.world.entity.Display.TextDisplay
import net.minecraft.world.entity.EntityType
import org.bukkit.Location
import xyz.xenondevs.nova.world.fakeentity.FakeEntity
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.TextDisplayMetadata

class FakeTextDisplay(
    location: Location,
    autoRegister: Boolean,
    beforeSpawn: ((FakeTextDisplay, TextDisplayMetadata) -> Unit)?,
    override val metadata: TextDisplayMetadata
) : FakeEntity<TextDisplayMetadata>(location) {
    
    override val entityType: EntityType<TextDisplay> = EntityType.TEXT_DISPLAY
    
    constructor(location: Location, autoRegister: Boolean = true, beforeSpawn: ((FakeTextDisplay, TextDisplayMetadata) -> Unit)? = null) :
        this(location, autoRegister, beforeSpawn, TextDisplayMetadata())
    
    init {
        beforeSpawn?.invoke(this, metadata)
        if (autoRegister) register()
    }
    
}