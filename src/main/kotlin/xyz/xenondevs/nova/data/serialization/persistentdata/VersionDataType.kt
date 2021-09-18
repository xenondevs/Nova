package xyz.xenondevs.nova.data.serialization.persistentdata

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.util.data.Version

object VersionDataType : PersistentDataType<String, Version> {
    override fun getPrimitiveType() = String::class.java
    
    override fun getComplexType() = Version::class.java
    
    override fun toPrimitive(version: Version, ctx: PersistentDataAdapterContext) = version.toString()
    
    override fun fromPrimitive(str: String, ctx: PersistentDataAdapterContext) = Version(str)
}