package xyz.xenondevs.nova.world.region

import xyz.xenondevs.nova.data.config.ValueReloadable
import java.util.*

class StaticRegion internal constructor(
    uuid: UUID,
    size: ValueReloadable<Int>,
    createRegion: (Int) -> Region,
) : ReloadableRegion(uuid, createRegion) {
    
    override val size by size
    
}