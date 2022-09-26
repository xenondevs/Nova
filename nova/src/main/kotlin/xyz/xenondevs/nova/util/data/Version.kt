package xyz.xenondevs.nova.util.data

import org.bukkit.Bukkit
import xyz.xenondevs.nova.util.mapToIntArray
import kotlin.math.max

class Version : Comparable<Version> {
    
    private val version: IntArray
    
    constructor(vararg version: Int) {
        this.version = version
    }
    
    constructor(version: String) {
        val split = version.removeSuffix("-SNAPSHOT").split('.')
        this.version = split.mapToIntArray { it.toIntOrNull() ?: 0 }
    }
    
    override fun toString(): String =
        toString(omitZeros = false)
    
    fun toString(separator: String = ".", omitZeros: Boolean = false): String {
        val sb = StringBuilder()
        for (i in 0..version.lastIndex) {
            sb.append(version[i])
            
            if (i != version.lastIndex) {
                if (omitZeros && version.copyOfRange(i + 1, version.size).all { it == 0 })
                    break
                sb.append(separator)
            }
        }
        
        return sb.toString()
    }
    
    override fun compareTo(other: Version) = compareTo(other, -1)
    
    /**
     * Compares this [Version] to [other].
     *
     * @param ignoreIdx Specifies after which version index the versions should not be compared.
     * (Example: with ignoreIdx = 2, 1.0.1 and 1.0.2 would be considered equal.)
     */
    fun compareTo(other: Version, ignoreIdx: Int): Int {
        val size = max(version.size, other.version.size)
        
        for (i in 0 until size) {
            if (i == ignoreIdx)
                return 0
            
            val myPart = version.getOrElse(i) { 0 }
            val otherPart = other.version.getOrElse(i) { 0 }
            
            val compare = myPart.compareTo(otherPart)
            if (compare != 0)
                return compare
        }
        
        return 0
    }
    
    operator fun rangeTo(other: Version): VersionRange = VersionRange(this, other)
    
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other !is Version)
            return false
        
        return equals(other, -1)
    }
    
    fun equals(other: Version, ignoreIdx: Int): Boolean =
        compareTo(other, ignoreIdx) == 0
    
    override fun hashCode(): Int {
        return version.contentHashCode()
    }
    
    companion object {
        
        val SERVER_VERSION: Version
        
        init {
            val versionPattern = Regex("""(1\.\d{1,2}(\.\d{1,2})?)""")
            SERVER_VERSION = Version(versionPattern.find(Bukkit.getVersion())!!.groupValues[1])
        }
        
    }
    
}

class VersionRange(val min: Version, val max: Version) : Comparable<VersionRange> {
    
    operator fun contains(version: Version) = version >= min && version <= max
    
    override fun compareTo(other: VersionRange): Int {
        return this.min.compareTo(other.min)
    }
    
    override fun toString(): String {
        return "$min <= version <= $max"
    }
    
}