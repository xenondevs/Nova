package xyz.xenondevs.nova.data.world.legacy

import org.bukkit.World
import java.io.File

internal abstract class VersionConverter {
    
    abstract fun getRegionFileConverter(world: World, old: File, new: File): RegionFileConverter
    
}

internal abstract class RegionFileConverter(val world: World, val old: File, val new: File) {
    
    abstract fun convert()
    
}