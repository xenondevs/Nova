package xyz.xenondevs.nova.util.data

import com.google.common.collect.HashBiMap
import org.bukkit.Bukkit
import xyz.xenondevs.nova.util.mapToIntArray
import kotlin.math.max

private val RELEASE_STAGES = HashBiMap.create<String, Int>().apply {
    this["rc"] = -1
    this["beta"] = -2
    this["alpha"] = -3
    this["snapshot"] = -4
}

private val VERSION_REGEX = Regex("""^([\d.]*)(-([a-z]*)(\d*))?$""")

class Version : Comparable<Version> {
    
    private val version: IntArray
    
    constructor(vararg version: Int) {
        this.version = version
    }
    
    constructor(version: String) {
        val result = VERSION_REGEX.matchEntire(version.lowercase())
            ?: throw IllegalArgumentException("${version.lowercase()} does not match version regex $VERSION_REGEX")
        
        val ver = result.groupValues[1]
        val stage = result.groupValues[3].takeUnless(String::isBlank)
        val stageNum = result.groupValues[4].takeUnless(String::isBlank)
        
        if (stage != null) {
            this.version = buildList<Int> {
                this += ver.split('.').map(String::toInt)
                this += RELEASE_STAGES[stage] ?: throw IllegalArgumentException("Unknown release stage: $stage")
                this += stageNum?.toInt() ?: 1
            }.toIntArray()
        } else {
            this.version = ver.split('.').mapToIntArray { it.toIntOrNull() ?: 0 }
        }
    }
    
    override fun toString(): String =
        toString(omitZeros = false)
    
    fun toString(separator: String = ".", omitZeros: Boolean = false): String {
        val sb = StringBuilder()
        
        var i = 0
        while (i <= version.lastIndex) {
            val v = version[i]
            if (v < 0) {
                val stage = RELEASE_STAGES.inverse()[v]
                if (stage != null) {
                    if (i > 0)
                        sb.deleteCharAt(sb.lastIndex)
                    sb.append("-$stage")
                    i++
                    sb.append(version.getOrNull(i) ?: 1)
                } else sb.append(v)
            } else sb.append(v)
            
            if (i < version.lastIndex) {
                if (omitZeros && version.copyOfRange(i + 1, version.size).all { it == 0 })
                    break
                sb.append(separator)
            }
            
            i++
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