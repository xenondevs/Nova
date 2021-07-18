package xyz.xenondevs.nova.region

import org.bukkit.Location
import org.bukkit.block.Block
import java.util.*

operator fun Location.rangeTo(loc: Location) = Region(this, loc)

class Region(val min: Location, val max: Location) : Iterable<Block> {
    
    val blocks: List<Block> by lazy {
        val blockList = ArrayList<Block>()
        for (x in min.blockX until max.blockX)
            for (y in min.blockY until max.blockY)
                for (z in min.blockZ until max.blockZ)
                    blockList.add(min.world!!.getBlockAt(x, y, z))
        return@lazy Collections.unmodifiableList(blockList)
    }
    
    val world by lazy { min.world }
    
    constructor(locations: Pair<Location, Location>) : this(locations.first, locations.second)
    
    init {
        require(min.world != null && min.world == max.world) { "Points must be in the same world." }
    }
    
    operator fun contains(loc: Location): Boolean {
        return loc.world == min.world
            && loc.x >= min.x && loc.x <= max.x
            && loc.y >= min.y && loc.y <= max.y
            && loc.z >= min.z && loc.z <= max.z
    }
    
    operator fun get(index: Int) = blocks[index]
    
    override fun iterator() = blocks.iterator()
}