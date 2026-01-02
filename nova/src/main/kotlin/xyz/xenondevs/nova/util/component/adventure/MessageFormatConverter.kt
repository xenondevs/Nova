package xyz.xenondevs.nova.util.component.adventure

import xyz.xenondevs.nova.util.component.adventure.MessageFormatConverter.escapeMessageFormatPattern
import java.text.MessageFormat

internal object MessageFormatConverter {
    
    /**
     * A regex matching a format string placeholder:
     *  - `%%` - literal percent sign (`%`)
     *  - `%s` - unindexed placeholder
     *  - `%<index>$s` - indexed placeholder (index is captured in group 1)
     */
    private val FORMAT_STRING_PLACEHOLDER_REGEX = Regex("""%%|%(?:(\d+)\$)?s""")
    
    /**
     * Escapes a string so it can be safely used as a pattern for [java.text.MessageFormat].
     * 
     * Returned pattern guarantees to format back to the original raw input using [java.text.MessageFormat.format].
     * 
     * Escaping rules:
     * - single quotes are doubled (`'` -> `''`)
     * - blocks of curly braces are enclosed with quotes (`{}` -> `'{}'`)
     */
    fun escapeMessageFormatPattern(raw: String): String {
        val result = StringBuilder()
        
        var inQuoteBlock = false
        for (char in raw) {
            when (char) {
                '{', '}' -> {
                    if (!inQuoteBlock) {
                        result.append('\'')
                        inQuoteBlock = true
                    }
                    result.append(char)
                }
                
                '\'' -> result.append("''") // double quotes even when currently in quoted block
                
                else -> {
                    if (inQuoteBlock) {
                        result.append('\'')
                        inQuoteBlock = false
                    }
                    result.append(char)
                }
            }
        }
        
        if (inQuoteBlock) result.append('\'') // end quote block if left open
        
        return result.toString()
    }
    
    /**
     * Transforms a format string into a [java.text.MessageFormat].
     * 
     * All special characters are escaped prior to placeholder conversion.
     * 
     * Placeholder conversions:
     * - `%%` -> `%`
     * - `%<index>$s` -> `{<index - 1>}`
     * - `%s` -> `{<automatic_index>}`
     * 
     * @see escapeMessageFormatPattern
     */
    fun formatStringToMessageFormat(formatString: String): MessageFormat {
        val escaped = escapeMessageFormatPattern(formatString)
        
        val result = StringBuilder()
        
        var autoPlaceholderIndex = 0
        var lastMatchIndex = 0
        for (match in FORMAT_STRING_PLACEHOLDER_REGEX.findAll(escaped)) {
            val range = match.range
            
            // add text before found match
            val textBeforeMatch = escaped.substring(lastMatchIndex, range.first)
            result.append(textBeforeMatch)
            
            lastMatchIndex = range.last + 1
            
            if (match.value == "%%") {
                // literal percent sign
                result.append('%')
                continue
            }
            
            val explicitPlaceholderIndex = match.groups[1] // index 0 is the entire match, groups start at 1
                ?.value?.toIntOrNull()
            
            val placeholderIndex = when {
                explicitPlaceholderIndex == null -> autoPlaceholderIndex++
                
                explicitPlaceholderIndex <= 0 -> {
                    // invalid explicit placeholder index, ignore
                    result.append(match.value)
                    continue
                }
                
                else -> explicitPlaceholderIndex - 1 // indexes in format strings start from 1
            }
            
            result.append('{').append(placeholderIndex).append('}')
        }
        
        // add text after last match
        result.append(escaped.substring(lastMatchIndex))
        
        return MessageFormat(result.toString())
    }
    
}