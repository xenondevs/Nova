package xyz.xenondevs.nova.data.world.legacy.impl.v0_10

import org.bukkit.Bukkit
import xyz.xenondevs.nova.data.world.RegionFile
import xyz.xenondevs.nova.data.world.legacy.RegionFileConverter
import xyz.xenondevs.nova.data.world.legacy.VersionConverter
import java.io.File

private val REGION_COORDS_REGEX = """r\.(-?\d+)\.(-?\d+)\.nvr""".toRegex()

internal object PreVarIntConverter : VersionConverter() {
    
    override fun getRegionFileConverter(old: File, new: File) = PreVarIntRegionConverter(old, new)
    
}

internal class PreVarIntRegionConverter(old: File, new: File) : RegionFileConverter(old, new) {
    
    override fun convert() {
        val regexGroups = REGION_COORDS_REGEX.matchEntire(new.name)!!.groupValues
        val regionX = regexGroups[1].toInt()
        val regionZ = regexGroups[2].toInt()
        val legacyRegion = LegacyRegionFile(old, regionX, regionZ).apply(LegacyRegionFile::init)
        val chunks = legacyRegion.readAllChunks()
        val newRegion = RegionFile(Bukkit.getWorlds()[0], new, regionX, regionZ)
        System.arraycopy(legacyRegion.chunks, 0, newRegion.chunks, 0, chunks.size)
        newRegion.save()
        legacyRegion.close()
    }
    
}