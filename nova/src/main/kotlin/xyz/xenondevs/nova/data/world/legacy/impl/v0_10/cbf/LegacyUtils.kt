@file:Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")

package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

@PublishedApi
@Deprecated("Legacy utility function")
internal inline fun <reified T> type(): Type = object : TypeToken<T>() {}.type