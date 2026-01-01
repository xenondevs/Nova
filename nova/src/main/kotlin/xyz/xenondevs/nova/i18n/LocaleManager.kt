package xyz.xenondevs.nova.i18n

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer
import net.kyori.adventure.translation.Translator
import net.minecraft.locale.Language
import net.minecraft.network.chat.FormattedText
import net.minecraft.util.FormattedCharSequence
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.data.readJson
import xyz.xenondevs.nova.util.formatSafely
import java.text.MessageFormat
import java.util.*
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC,
    dependsOn = [ResourceGeneration.PostWorld::class]
)
object LocaleManager {
    
    private val RENDERER = TranslatableComponentRenderer.usingTranslationSource(NovaTranslator)
    
    private var vanillaTranslations: Map<String, Map<String, String>> = emptyMap()
    
    @InitFun
    private suspend fun init() = withContext(Dispatchers.IO) {
        vanillaTranslations = ResourcePackBuilder.MCASSETS_DIR.resolve("assets/minecraft/lang/").walk()
            .filter { !it.isDirectory() && it.extension.equals("json", true) }
            .filter { it.name != "deprecated.json" }
            .associate { file -> file.nameWithoutExtension to async { file.readJson<Map<String, String>>() } }
            .toMap()
            .mapValues { (_, v) -> v.await() }
        
        Language.inject(NovaLanguage)
    }
    
    /**
     * Checks whether Nova is aware of a translation for [key] in [lang].
     */
    fun hasTranslation(lang: String, key: String): Boolean {
        if (ResourceLookups.LANGUAGE[lang.lowercase()]?.contains(key) == true)
            return true
        return vanillaTranslations[lang.lowercase()]?.contains(key) == true
    }
    
    /**
     * Gets the format string for [key] in [lang], or null if none was found.
     */
    fun getFormatStringOrNull(lang: String, key: String): String? {
        return ResourceLookups.LANGUAGE[lang.lowercase()]?.get(key) 
            ?: vanillaTranslations[lang.lowercase()]?.get(key)
    }
    
    /**
     * Gets the format string for [key] in [lang], falling back the format string
     * to `en_us` or the [key] itself if none was found.
     */
    fun getFormatString(lang: String, key: String): String {
        var formatString = getFormatStringOrNull(lang, key)
        if (formatString == null && lang != "en_us")
            formatString = getFormatStringOrNull("en_us", key)
        return formatString ?: key
    }
    
    /**
     * Gets the translation for [key] in [lang] using [args], or null if there is no format string.
     */
    fun getTranslationOrNull(lang: String, key: String, vararg args: Any): String? {
        return getFormatStringOrNull(lang, key)?.let { String.formatSafely(it, *args) }
    }
    
    /**
     * Gets the translation for [key] in [lang] using [args], falling back the translation
     * to `en_us` or the [key] itself if none was found.
     */
    fun getTranslation(lang: String, key: String, vararg args: Any): String {
        var translation = getTranslationOrNull(lang, key, *args)
        if (translation == null && lang != "en_us")
            translation = getTranslationOrNull("en_us", key, *args)
        return translation ?: key
    }
    
    /**
     * Renders the translatable parts of [component] in the given [locale].
     */
    fun render(component: Component, locale: Locale): Component {
        return RENDERER.render(component, locale)
    }
    
    private object NovaLanguage : Language() {
        
        private val delegate = getInstance()
        
        override fun getOrDefault(key: String): String {
            return getTranslationOrNull("en_us", key) ?: delegate.getOrDefault(key)
        }
        
        override fun getOrDefault(key: String, fallback: String): String {
            return getTranslationOrNull("en_us", key) ?: delegate.getOrDefault(key, fallback)
        }
        
        override fun has(key: String): Boolean {
            return hasTranslation("en_us", key) || delegate.has(key)
        }
        
        override fun isDefaultRightToLeft(): Boolean {
            return delegate.isDefaultRightToLeft
        }
        
        override fun getVisualOrder(text: FormattedText): FormattedCharSequence {
            return delegate.getVisualOrder(text)
        }
        
    }
    
    private object NovaTranslator : Translator {
        
        override fun name(): Key = Key.key("nova", "translator")
        
        override fun translate(key: String, locale: Locale): MessageFormat? {
            val lang = buildString { 
                append(locale.language.lowercase())
                if (locale.country.isNotEmpty()) {
                    append("_")
                    append(locale.country.lowercase())
                    if (locale.variant.isNotEmpty()) {
                        append("_")
                        append(locale.variant.lowercase())
                    }
                }
            }
            
            return getFormatStringOrNull(lang, key)?.let(MessageFormatConverter::formatStringToMessageFormat)
        }
        
    }
        
}

object MessageFormatConverter {
    /**
     * A regex matching a format string placeholder:
     *  - `%%` - literal percent sign (`%`)
     *  - `%s` - unindexed placeholder
     *  - `%<index>$s` - indexed placeholder (index is captured in group 1)
     */
    private val FORMAT_STRING_PLACEHOLDER_REGEX = Regex("""%%|%(?:(\d+)\$)?s""")

    /**
     * Escapes a string so it can be safely used as a pattern for [MessageFormat].
     * 
     * Returned pattern guarantees to format back to the original raw input using [MessageFormat.format].
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
     * Transforms a format string into a [MessageFormat].
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
