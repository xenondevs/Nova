package xyz.xenondevs.nova.util.data

import org.bukkit.Bukkit

class Version : Comparable<Version> {
    
    val major: Int
    val minor: Int
    val patch: Int
    
    constructor(major: Int, minor: Int, patch: Int) {
        this.major = major
        this.minor = minor
        this.patch = patch
    }
    
    constructor(version: String) {
        val split = version.removeSuffix("-SNAPSHOT").split('.')
        this.major = split.getOrNull(0)?.toIntOrNull() ?: 0
        this.minor = split.getOrNull(1)?.toIntOrNull() ?: 0
        this.patch = split.getOrNull(2)?.toIntOrNull() ?: 0
    }
    
    override fun toString() = "$major.$minor.$patch"
    
    override fun compareTo(other: Version) = compareTo(other, false)
    
    fun compareTo(other: Version, ignorePatches: Boolean): Int {
        if (this.major < other.major || this.minor < other.minor || (!ignorePatches && this.patch < other.patch))
            return -1
        if (this.major > other.major || this.minor > other.minor || (!ignorePatches && this.patch > other.patch))
            return 1
        return 0
    }
    
    operator fun rangeTo(other: Version): VersionRange = VersionRange(this, other)
    
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other !is Version)
            return false
        return this.major == other.major
            && this.minor == other.minor
            && this.patch == other.patch
    }
    
    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        return result
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
    
}