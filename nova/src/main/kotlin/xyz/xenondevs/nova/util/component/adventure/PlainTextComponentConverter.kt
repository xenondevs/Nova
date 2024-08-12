package xyz.xenondevs.nova.util.component.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.flattener.ComponentFlattener
import xyz.xenondevs.nova.i18n.LocaleManager

// TODO: use built-in component renderer
object PlainTextComponentConverter {
    
    private val flatteners = HashMap<String, ComponentFlattener>()
    
    fun toPlainText(component: Component, lang: String): String {
        val flattener = flatteners.computeIfAbsent(lang, ::createFlattener)
        val sb = StringBuilder()
        flattener.flatten(component, sb::append)
        return sb.toString()
    }
    
    private fun createFlattener(lang: String): ComponentFlattener =
        ComponentFlattener.builder()
            .mapper(TextComponent::class.java, TextComponent::content)
            .mapper(TranslatableComponent::class.java) { tcomp ->
                LocaleManager.getTranslation(
                    lang,
                    tcomp.key(),
                    tcomp.arguments().map { arg -> toPlainText(arg.asComponent(), lang) }
                )
            }.build()
    
}