package xyz.xenondevs.nova.i18n

import org.junit.jupiter.api.Test
import java.text.MessageFormat
import kotlin.test.assertEquals

class MessageFormatConverterTest {
    @Test
    fun `test pattern escaping`() {
        val inputs = listOf(
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

        inputs.forEach { input ->
            val pattern = MessageFormatConverter.escapeMessageFormatPattern(input)
            val output = MessageFormat(pattern).format(emptyArray<String>())
            assertEquals(input, output, "Failed for: '$input'")
        }
    }

    @Test
    fun `test format string to message format conversion`() {
        val conversions = listOf<Triple<String, List<String>, String>>(
            // Triple(input, args, expected)
            Triple("", emptyList(), ""),
            Triple("Hello World", emptyList(), "Hello World"),
            // test excess arguments
            Triple("Hello World", listOf("A", "B", "C"), "Hello World"),
            // test escaping
            Triple("'Hello' {0} '{{'}}'", emptyList(), "'Hello' {0} '{{'}}'"),
            // test placeholder
            Triple("Hello %s", listOf("World"), "Hello World"),
            // test placeholder with explicit index
            Triple($$"Hello %s, %1$s, %s", listOf("World", "Universe"), "Hello World, World, Universe"),
            // test literal percentage sign
            Triple("Percentage: %s%%", listOf("50"), "Percentage: 50%"),
            // test invalid placeholders 
            Triple(
                $$"Hello %0$s %-1$s %3.14$s %d",
                listOf("World", "Universe"),
                $$"Hello %0$s %-1$s %3.14$s %d"
            )
        )

        for ((input, args, expected) in conversions) {
            val output = MessageFormatConverter.formatStringToMessageFormat(input)
                .format(args.toTypedArray())
            assertEquals(expected, output, "Failed for: '$input'")
        }
    }
}