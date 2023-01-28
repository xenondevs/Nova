package xyz.xenondevs.nova.util

import java.util.*

fun UUID.salt(salt: String): UUID = UUID.nameUUIDFromBytes((this.toString() + salt).toByteArray())

object UUIDUtils {
    
    val ZERO = UUID(0, 0)
    
}