package xyz.xenondevs.nova.data.world.legacy.impl.v0_10

import org.bukkit.World
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.world.RegionFile
import xyz.xenondevs.nova.data.world.legacy.RegionFileConverter
import xyz.xenondevs.nova.data.world.legacy.VersionConverter
import java.io.File

private val REGION_COORDS_REGEX = """r\.(-?\d+)\.(-?\d+)\.nvr""".toRegex()

internal object PreVarIntConverter : VersionConverter() {
    
    override fun getRegionFileConverter(world: World, old: File, new: File): RegionFileConverter = PreVarIntRegionConverter(world, old, new)
    
}

internal class PreVarIntRegionConverter(world: World, old: File, new: File) : RegionFileConverter(world, old, new) {
    
    override fun convert() {
        val regexGroups = REGION_COORDS_REGEX.matchEntire(new.name)!!.groupValues
        val regionX = regexGroups[1].toInt()
        val regionZ = regexGroups[2].toInt()
        val legacyRegion = LegacyRegionFile(old, regionX, regionZ).apply(LegacyRegionFile::init)
        legacyRegion.readAllChunks()
        val newRegion = RegionFile(world, new, regionX, regionZ)
        System.arraycopy(legacyRegion.chunks, 0, newRegion.chunks, 0, legacyRegion.chunks.size)
        PermanentStorage.store("legacyNetworkChunks", legacyRegion.chunks.mapNotNull { it?.pos })
        newRegion.save()
        legacyRegion.close()
    }
    
}