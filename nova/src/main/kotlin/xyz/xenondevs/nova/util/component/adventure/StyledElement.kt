package xyz.xenondevs.nova.util.component.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ObjectComponent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.TranslationArgument
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.`object`.ObjectContents
import xyz.xenondevs.nova.i18n.LocaleManager
import kotlin.streams.asSequence

/**
 * Represents a renderable element of a [Component] with an associated [Style].
 */
sealed interface StyledElement {
    
    /**
     * The [Style] associated with this element.
     */
    val style: Style
    
    /**
     * A code point (character).
     */
    data class CodePoint(
        override val style: Style,
        val codePoint: Int
    ) : StyledElement
    
    /**
     * An object component.
     */
    data class Object(
        override val style: Style,
        val contents: ObjectContents
    ) : StyledElement
    
}

/**
 * Flattens this [Component] into a sequence of [StyledElements][StyledElement] using the specified [lang] for translations.
 * 
 * Supported component types: [TextComponent], [TranslatableComponent], [ObjectComponent].
 * Other component types will be ignored.
 */
fun Component.elements(lang: String = "en_us"): Sequence<StyledElement> =
    elements({ LocaleManager.getFormatStringOrNull(lang, it) }, Style.empty())

/**
 * Flattens this [Component] into a sequence of [StyledElements][StyledElement] using the specified [translate] function for translations.
 * An optional [outerStyle] can be provided to be merged with each element's style.
 * 
 * Supported component types: [TextComponent], [TranslatableComponent], [ObjectComponent].
 * Other component types will be ignored.
 */
fun Component.elements(
    translate: (key: String) -> String?,
    outerStyle: Style = Style.empty()
): Sequence<StyledElement> = sequence {
    val style = style().merge(outerStyle)
    when (this@elements) {
        is TextComponent -> yieldAll(content().styledCodePoints(style))
        is ObjectComponent -> yield(StyledElement.Object(style, contents()))
        is TranslatableComponent -> {
            val fstr = translate(key()) ?: fallback()
            if (fstr != null) {
                yieldAll(elements(translate, fstr, arguments(), style))
            } else {
                yieldAll(key().styledCodePoints(style))
            }
        }
    }
    for (child in children()) {
        yieldAll(child.elements(translate, style))
    }
}

private val FSTR_PATTERN = Regex("""%(?:(\d+)\$)?(.|$)""")

private fun elements(
    translate: (key: String) -> String?,
    fstr: String,
    args: List<TranslationArgument>,
    style: Style
): Sequence<StyledElement> {
    val matches = FSTR_PATTERN.findAll(fstr).toList()
    if (checkFstrIsInvalid(matches, args.size))
        return fstr.styledCodePoints(style)
    
    return sequence {
        var currentArgIdx = 0
        var i = 0
        
        FSTR_PATTERN.findAll(fstr).forEach { result ->
            yieldAll(fstr.substring(i, result.range.first).styledCodePoints(style))
            
            val (explicitArgIdx, formatType) = result.destructured
            when (formatType) {
                "%" -> yield(StyledElement.CodePoint(style, '%'.code))
                "s" if explicitArgIdx.isNotEmpty() -> {
                    val arg = explicitArgIdx.toIntOrNull()
                        ?.let { args.getOrNull(it - 1) }
                        ?.asComponent()
                        ?: Component.empty()
                    yieldAll(arg.elements(translate, style))
                }
                
                "s" -> {
                    val arg = args.getOrNull(currentArgIdx++)?.asComponent() ?: Component.empty()
                    yieldAll(arg.elements(translate, style))
                }
            }
            
            i = result.range.last + 1
        }
        
        if (i < fstr.length) {
            yieldAll(fstr.substring(i).styledCodePoints(style))
        }
    }
}

private fun checkFstrIsInvalid(matches: List<MatchResult>, args: Int): Boolean {
    var currentArgIdx = 0
    for (result in matches) {
        val (explicitArgIdx, formatType) = result.destructured
        
        if (formatType != "%" && formatType != "s")
            return true
        
        if (explicitArgIdx.isNotEmpty()) {
            val explicitArgIdx = explicitArgIdx.toIntOrNull()
            if (explicitArgIdx == null || explicitArgIdx !in 1..args)
                return true
        } else if (formatType == "s" && currentArgIdx++ >= args) {
            return true
        }
    }
    
    return false
}

private fun String.styledCodePoints(style: Style): Sequence<StyledElement.CodePoint> =
    codePoints().mapToObj { StyledElement.CodePoint(style, it) }.asSequence()