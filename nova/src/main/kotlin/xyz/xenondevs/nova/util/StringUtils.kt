package xyz.xenondevs.nova.util

import java.util.*

private val FORMATTING_FILTER_REGEX = Regex("§.")

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }"))
fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

internal fun String.pluralize(): String = when {
    endsWith("s") -> this
    endsWith("ch") || endsWith("sh") || endsWith("x") || endsWith("z") -> "${this}es"
    endsWith("y") && length > 1 && this[length - 2] !in "aeiou" -> dropLast(1) + "ies"
    else -> "${this}s"
}

@Deprecated(DEBLOAT_DEPRECATE)
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

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("StringBuilder(this).insert(offset, charSequence).toString()"))
fun String.insert(offset: Int, charSequence: CharSequence) = StringBuilder(this).insert(offset, charSequence).toString()

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("insert(indexOf(char, ignoreCase = ignoreCase) + 1, charSequence)"))
fun String.insertAfter(char: Char, charSequence: CharSequence, ignoreCase: Boolean = false) =
    insert(indexOf(char, ignoreCase = ignoreCase) + 1, charSequence)

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("insert(lastIndexOf(char, ignoreCase = ignoreCase) + 1, charSequence)"))
fun String.insertAfterLast(char: Char, charSequence: CharSequence, ignoreCase: Boolean = false) =
    insert(lastIndexOf(char, ignoreCase = ignoreCase) + 1, charSequence)

@Deprecated(DEBLOAT_DEPRECATE)
fun String.Companion.formatSafely(format: String, vararg args: Any?): String {
    return try {
        String.format(format, *args)
    } catch (e: IllegalFormatException) {
        format
    }
}

@Deprecated(DEBLOAT_DEPRECATE)
fun String.removeMinecraftFormatting(): String {
    return replace(FORMATTING_FILTER_REGEX, "")
}

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith($$"if (startsWith(prefix)) this else \"$prefix$this\""))
fun String.addPrefix(prefix: String): String =
    if (startsWith(prefix)) this else "$prefix$this"

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith($$"if (endsWith(suffix)) this else \"$this$suffix\""))
fun String.addSuffix(suffix: String): String =
    if (endsWith(suffix)) this else "$this$suffix"

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith($$"addPrefix(\"$namespace:\")"))
fun String.addNamespace(namespace: String): String =
    addPrefix("$namespace:")

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith($$"removePrefix(\"$namespace:\")"))
fun String.removeNamespace(namespace: String): String =
    removePrefix("$namespace:")

@Deprecated(DEBLOAT_DEPRECATE)
fun String.startsWithAny(vararg prefixes: String): Boolean {
    for (prefix in prefixes)
        if (startsWith(prefix)) return true
    return false
}

@Deprecated(DEBLOAT_DEPRECATE)
fun String.equalsAny(vararg strings: String, ignoreCase: Boolean = false): Boolean {
    for (string in strings)
        if (equals(string, ignoreCase)) return true
    return false
}

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("this.toString() + other"))
operator fun Any.plus(other: String) = this.toString() + other

@Deprecated(DEBLOAT_DEPRECATE)
object StringUtils {
    
    val UPPER_CASE_ALPHABET = CharArray(26) { 'A' + it }
    val LOWER_CASE_ALPHABET = CharArray(26) { 'a' + it }
    val ALPHABET = UPPER_CASE_ALPHABET + LOWER_CASE_ALPHABET
    
    fun randomString(length: Int, dict: CharArray = ALPHABET) =
        buildString {
            repeat(length) {
                append(dict.random())
            }
        }
}