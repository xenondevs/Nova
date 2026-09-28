package xyz.xenondevs.nova.util

internal fun <T> ScopedValue<T>.getOrNull(): T? =
    if (isBound) get() else null

internal fun <R>  ScopedValue.Carrier.exec(fn: () -> R): R =
    call<R, Nothing>(fn)