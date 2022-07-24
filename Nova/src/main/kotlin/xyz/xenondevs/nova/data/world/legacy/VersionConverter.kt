package xyz.xenondevs.nova.data.world.legacy

import java.io.File

internal abstract class VersionConverter {
    
    abstract fun getRegionFileConverter(old: File, new: File): RegionFileConverter
    
}

internal abstract class RegionFileConverter(val old: File, val new: File) {
    
    abstract fun convert()
    
}