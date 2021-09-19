package xyz.xenondevs.nova.util.data

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
        val split = version.split('.')
        this.major = split.getOrNull(0)?.toInt() ?: 0
        this.minor = split.getOrNull(1)?.toInt() ?: 0
        this.patch = split.getOrNull(2)?.toInt() ?: 0
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
    
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other !is Version)
            return false
        return this.compareTo(other) == 0
    }
    
    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        return result
    }
    
}