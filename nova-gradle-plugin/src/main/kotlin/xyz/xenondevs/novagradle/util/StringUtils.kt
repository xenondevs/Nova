package xyz.xenondevs.novagradle.util

internal fun String.toClassFilePath(): String = replace('.', '/') + ".class"