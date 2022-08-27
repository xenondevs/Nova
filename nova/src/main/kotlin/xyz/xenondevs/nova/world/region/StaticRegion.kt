package xyz.xenondevs.nova.world.region

import xyz.xenondevs.nova.data.config.ValueReloadable
import java.util.*

class StaticRegion internal constructor(
    uuid: UUID,
    size: ValueReloadable<Int>,
    private val createRegion: (Int) -> Region,
) : ReloadableRegion(uuid) {
    
    val size by size
    
    override var region: Region = createRegion(this.size)
        private set
    
    override fun reload() {
        region = createRegion(this.size)
        VisualRegion.updateRegion(uuid, region)
    }
    
}