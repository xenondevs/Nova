package xyz.xenondevs.nova.world.region

import xyz.xenondevs.commons.provider.Provider
import java.util.*

class StaticRegion internal constructor(
    uuid: UUID,
    size: Provider<Int>,
    createRegion: (Int) -> Region,
) : ReloadableRegion(uuid, createRegion) {
    
    override val size by size
    
}