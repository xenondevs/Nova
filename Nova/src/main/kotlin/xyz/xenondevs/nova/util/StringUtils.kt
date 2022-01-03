package xyz.xenondevs.nova.util

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

fun String.capitalizeAll(): String {
    if (length == 0) return this
    
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