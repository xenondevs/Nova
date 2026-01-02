package xyz.xenondevs.nova.util.component.adventure

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.text.MessageFormat
import kotlin.test.assertEquals

class MessageFormatConverterTest {
    
    companion object {
        
        @JvmStatic
        fun patternEscapingInputs() = listOf(
            "",
            "Hello World",
            "'",
            "'''",
            "{}",
            "{0}{1}",
            "{{}}",
            "'{{'}}'",
            "'{{'Hello'}}' '{{'World'}}'"
        )
        
        @JvmStatic
        fun formatStringConversionInputs() = listOf(
            // Arguments(input, args, expected)
            Arguments.of("", emptyList<String>(), ""),
            Arguments.of("Hello World", emptyList<String>(), "Hello World"),
            // test excess arguments
            Arguments.of("Hello World", listOf("A", "B", "C"), "Hello World"),
            // test escaping
            Arguments.of("'Hello' {0} '{{'}}'", emptyList<String>(), "'Hello' {0} '{{'}}'"),
            // test placeholder
            Arguments.of("Hello %s", listOf("World"), "Hello World"),
            // test placeholder with explicit index
            Arguments.of($$"Hello %s, %1$s, %s", listOf("World", "Universe"), "Hello World, World, Universe"),
            // test literal percentage sign
            Arguments.of("Percentage: %s%%", listOf("50"), "Percentage: 50%"),
            // test invalid placeholders 
            Arguments.of(
                $$"Hello %0$s %-1$s %3.14$s %d",
                listOf("World", "Universe"),
                $$"Hello %0$s %-1$s %3.14$s %d"
            )
        )
        
    }
    
    @ParameterizedTest
    @MethodSource("patternEscapingInputs")
    fun `test pattern escaping`(input: String) {
        val pattern = MessageFormatConverter.escapeMessageFormatPattern(input)
        val output = MessageFormat(pattern).format(emptyArray<String>())
        assertEquals(input, output)
    }

    @ParameterizedTest
    @MethodSource("formatStringConversionInputs")
    fun `test format string to message format conversion`(input: String, args: List<String>, expected: String) {
        val output = MessageFormatConverter.formatStringToMessageFormat(input)
            .format(args.toTypedArray())
        assertEquals(expected, output)
    }
    
}