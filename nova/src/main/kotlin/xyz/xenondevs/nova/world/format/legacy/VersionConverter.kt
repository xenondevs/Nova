package xyz.xenondevs.nova.world.format.legacy

import org.bukkit.World
import java.io.File

internal abstract class VersionConverter {
    
    abstract fun getRegionFileConverter(world: World, old: File, new: File): RegionFileConverter
    
    abstract fun handleRegionFilesConverted()
    
}

internal abstract class RegionFileConverter(val world: World, val old: File, val new: File) {
    
    abstract fun convert()
    
}