package xyz.xenondevs.nova.util

import java.util.*

// TODO: move to commons

private val FORMATTING_FILTER_REGEX = Regex("§.")

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

fun String.capitalizeAll(): String {
    if (isEmpty()) return this
    
    val chars = toCharArray()
    chars[0] = chars[0].uppercaseChar()
    for (i in chars.indices) {
        if ((i + 1) >= chars.size) break
        
        val char = chars[i]
        if (char == ' ') {
            chars[i + 1] = chars[i + 1].uppercaseChar()
        }
    }
    
    return String(chars)
}

fun String.insert(offset: Int, charSequence: CharSequence) = StringBuilder(this).insert(offset, charSequence).toString()

fun String.insertAfter(char: Char, charSequence: CharSequence, ignoreCase: Boolean = false) =
    insert(indexOf(char, ignoreCase = ignoreCase) + 1, charSequence)

fun String.insertAfterLast(char: Char, charSequence: CharSequence, ignoreCase: Boolean = false) =
    insert(lastIndexOf(char, ignoreCase = ignoreCase) + 1, charSequence)

fun String.Companion.formatSafely(format: String, vararg args: Any?): String {
    return try {
        String.format(format, *args)
    } catch (e: IllegalFormatException) {
        format
    }
}

fun String.removeMinecraftFormatting(): String {
    return replace(FORMATTING_FILTER_REGEX, "")
}

fun String.addPrefix(prefix: String): String =
    if (startsWith(prefix)) this else "$prefix$this"

fun String.addSuffix(suffix: String): String =
    if (endsWith(suffix)) this else "$this$suffix"

fun String.addNamespace(namespace: String): String =
    addPrefix("$namespace:")

fun String.removeNamespace(namespace: String): String =
    removePrefix("$namespace:")

fun String.startsWithAny(vararg prefixes: String): Boolean {
    for (prefix in prefixes)
        if (startsWith(prefix)) return true
    return false
}

fun String.equalsAny(vararg strings: String, ignoreCase: Boolean = false): Boolean {
    for (string in strings)
        if (equals(string, ignoreCase)) return true
    return false
}

operator fun Any.plus(other: String) = this.toString() + other

object StringUtils {
    
    private val ALPHABET = ('a'..'z') + ('A'..'Z')
    
    fun randomString(length: Int, dict: List<Char> = ALPHABET) =
        buildString {
            repeat(length) {
                append(dict.random())
            }
        }
}