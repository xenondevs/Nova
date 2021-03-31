package xyz.xenondevs.nova.util

import java.util.*

fun UUID.seed(seed: String): UUID = UUID.nameUUIDFromBytes((this.toString() + seed).toByteArray())