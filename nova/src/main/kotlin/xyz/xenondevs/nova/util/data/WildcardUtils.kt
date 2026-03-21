package xyz.xenondevs.nova.util.data

internal object WildcardUtils {
    
    private val ESCAPE_REGEX_CHARS = "\\^\$*+?.()|{}[]".toCharArray()
    private val WILDCARD_REPLACEMENT_RULES = mapOf('*' to ".*", '?' to ".")
    
    fun toRegex(wildcard: String): Regex {
        return Regex(toRegexString(wildcard))
    }
    
    fun toRegexString(wildcard: String): String {
        val regexStr = StringBuilder()
        wildcard.toCharArray().forEach { char ->
            regexStr.append(
                WILDCARD_REPLACEMENT_RULES[char]
                    ?: if (char in ESCAPE_REGEX_CHARS) "\\$char" else char
            )
        }
        
        return regexStr.toString()
    }
    
}