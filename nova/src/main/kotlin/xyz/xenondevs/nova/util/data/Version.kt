package xyz.xenondevs.nova.util.data

import com.google.common.collect.HashBiMap
import org.bukkit.Bukkit
import xyz.xenondevs.commons.collections.mapToIntArray
import kotlin.math.max

private val RELEASE_STAGES = HashBiMap.create<String, Int>().apply {
    this["rc"] = -1
    this["beta"] = -2
    this["alpha"] = -3
    this["snapshot"] = -4
}

private val VERSION_REGEX = Regex("""^([\d.]+)(?:-(snapshot|alpha|beta|rc)(?:\.?([\d.]+))?)?$""")

class Version : Comparable<Version> {
    
    private val version: IntArray
    private val stageVersion: IntArray
    
    val isFullRelease: Boolean
        get() = stageVersion.isEmpty()
    
    constructor(vararg version: Int) {
        this.version = version
        this.stageVersion = intArrayOf()
    }
    
    @Suppress("RemoveExplicitTypeArguments")
    constructor(version: String) {
        val result = VERSION_REGEX.matchEntire(version.lowercase())
            ?: throw IllegalArgumentException("${version.lowercase()} does not match version regex $VERSION_REGEX")
        
        val ver = result.groupValues[1]
        val stage = result.groupValues[2].takeUnless(String::isBlank)
        val stageVer = result.groupValues[3].takeUnless(String::isBlank)
        
        this.version = ver.split('.').mapToIntArray { it.toIntOrNull() ?: 0}
        
        if (stage != null) {
            this.stageVersion = buildList<Int> {
                this += RELEASE_STAGES[stage] ?: throw IllegalArgumentException("Unknown release stage: $stage")
                if (stageVer != null) this += stageVer.split('.').map { it.toIntOrNull() ?: 0 }
            }.toIntArray()
        } else this.stageVersion = intArrayOf()
    }
    
    override fun toString(): String =
        toString(omitZeros = false)
    
    fun toString(separator: String = ".", omitZeros: Boolean = false): String {
        val sb = StringBuilder()
        
        fun isAllZeros(start: Int, array: IntArray) =
            array.copyOfRange(start, array.size).all { it == 0 }
        
        fun appendVersion(start: Int, array: IntArray) {
            for (i in start..array.lastIndex) {
                sb.append(array[i])
                if (i < array.lastIndex) {
                    if (omitZeros && isAllZeros(i + 1, array))
                        break
                    sb.append(separator)
                }
            }
        }
        
        appendVersion(0, version)
        
        if (stageVersion.isNotEmpty()) {
            sb.append("-")
            sb.append(RELEASE_STAGES.inverse()[stageVersion[0]])
            if (stageVersion.size > 1 && (!omitZeros || !isAllZeros(1, stageVersion))) {
                sb.append(".")
                appendVersion(1, stageVersion)
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
        val compare = compareVersionArray(version, other.version, ignoreIdx)
        if (compare != 0 || ignoreIdx != -1) // only compare stage version if an exact comparison (ignoreIdx = -1) was requested
            return compare
        
        return compareVersionArray(stageVersion, other.stageVersion, -1)
    }
    
    private fun compareVersionArray(a: IntArray, b: IntArray, ignoreIdx: Int): Int {
        val size = max(a.size, b.size)
        
        for (i in 0 until size) {
            if (i == ignoreIdx)
                return 0
            
            val myPart = a.getOrElse(i) { 0 }
            val otherPart = b.getOrElse(i) { 0 }
            
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