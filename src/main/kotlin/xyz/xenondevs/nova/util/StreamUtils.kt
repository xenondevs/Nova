package xyz.xenondevs.nova.util

import java.util.stream.Stream

operator fun <T> Stream<out T>.plus(other: Stream<out T>): Stream<out T> {
    return Stream.concat(this, other)
}