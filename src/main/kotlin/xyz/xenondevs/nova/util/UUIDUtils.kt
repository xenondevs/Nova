package xyz.xenondevs.nova.util

import java.util.*

fun UUID.salt(salt: String): UUID = UUID.nameUUIDFromBytes((this.toString() + salt).toByteArray())