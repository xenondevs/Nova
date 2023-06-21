@file:Suppress("LiftReturnOrAssignment")

package xyz.xenondevs.nova.util.component.adventure

import it.unimi.dsi.fastutil.Stack
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntStack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.Style
import xyz.xenondevs.nova.i18n.LocaleManager

// TODO: code points instead of chars

fun Component.chars(lang: String = "en_us"): Sequence<StylizedChar> =
    ComponentCharsIterator(null, this, lang).asSequence()

fun Component.charsIterator(lang: String = "en_us"): Iterator<StylizedChar> =
    ComponentCharsIterator(null, this, lang)

data class StylizedChar(
    val char: Char,
    val style: Style
)

private class ComponentCharsIterator(
    outerStyle: Style?,
    component: Component,
    val lang: String
) : Iterator<StylizedChar> {
    
    // stacks
    private val components: Stack<Component> = ObjectArrayList()
    private val styles: Stack<Style> = ObjectArrayList()
    private val childIndices: IntStack = IntArrayList()
    private val argIndices: IntStack = IntArrayList()
    private val contentStrings: Stack<String> = ObjectArrayList()
    private val readHeads: IntStack = IntArrayList() // Stores the read heads for the previous components, not the current one
    
    // variables currently in use, prevents unnecessary stack operations
    private lateinit var component: Component
    private lateinit var content: String
    private lateinit var style: Style
    private var readHead: Int = -1
    
    init {
        // push outer style
        styles.push(outerStyle ?: Style.empty())
        
        // push initial component
        pushComponent(component)
    }
    
    override fun hasNext(): Boolean = !components.isEmpty
    
    override fun next(): StylizedChar {
        // read char
        var char: Char?
        do {
            if (components.isEmpty)
                throw NoSuchElementException()
            
            char = when (component) {
                is TextComponent -> nextTextChar()
                is TranslatableComponent -> nextTranslatableChar()
                else -> throw UnsupportedOperationException("Unsupported component type: ${component::class.qualifiedName}")
            }
        } while (char == null)
        
        // store style in local variable before it gets updated by nextComponent()
        val style = style
        
        // select next component if content string is fully read
        if (!hasNextChar())
            nextComponent()
        
        return StylizedChar(char, style)
    }
    
    private fun hasNextChar(): Boolean {
        return readHead < content.length
    }
    
    private fun nextTextChar(): Char {
        return content[readHead++]
    }
    
    private fun nextTranslatableChar(): Char? {
        val fstr = content
        
        // Valid formats are:
        // %% - percent symbol
        // %s - next argument
        // %<index>$s - argument at index
        // The equivalent regex is:
        // %(?:(\d+)\$)?([A-Za-z%]|$)
        
        val char = fstr[readHead]
        if (char == '%') {
            if (fstr.lastIndex > readHead && fstr[readHead + 1] == '%') { // percent symbol
                readHead += 2
                return '%' // return single percent symbol
            } else if (fstr.lastIndex > readHead && fstr[readHead + 1] == 's') { // %s
                readHead += 2
                nextArg()
                return null // signals that a new component has been pushed
            } else if (fstr.lastIndex > readHead + 2 && fstr[readHead + 1].isDigit() && fstr[readHead + 2] == '$' && fstr[readHead + 3] == 's') { // %<index>$s
                readHead += 4
                pushArg(fstr[readHead + 1].digitToInt())
                return null // signals that a new component has been pushed
            } else throw IllegalArgumentException("Invalid format string: $fstr")
        } else {
            readHead++
            return char // normal char
        }
    }
    
    private fun nextComponent() {
        do {
            // find next child of top component
            val children = components.top().children()
            val childIdx = childIndices.popInt() + 1
            if (childIdx <= children.lastIndex) {
                childIndices.push(childIdx)
                val child = children[childIdx]
                pushComponent(child)
                break
            } else {
                // if this component and its children are fully read, pop it
                components.pop()
                contentStrings.pop()
                styles.pop()
                argIndices.popInt()
    
                // try to continue reading the new top component (for translatable components)
                if (!readHeads.isEmpty) {
                    readHead = readHeads.popInt()
                    content = contentStrings.top()
                    if (readHead < content.length) {
                        this.component = components.top()
                        this.style = styles.top()
                        break
                    }
                }
            }
        } while (!components.isEmpty)
    }
    
    private fun nextArg() {
        val args = (components.top() as TranslatableComponent).args()
        val argIdx = argIndices.popInt() + 1
        argIndices.push(argIdx)
        val arg = args.getOrNull(argIdx) ?: Component.empty()
        pushComponent(arg)
    }
    
    private fun pushArg(argIdx: Int) {
        val args = (components.top() as TranslatableComponent).args()
        val arg = args.getOrNull(argIdx) ?: Component.empty()
        pushComponent(arg)
    }
    
    private fun pushComponent(component: Component) {
        // push component & indices
        components.push(component)
        childIndices.push(-1)
        argIndices.push(-1)
        
        if (readHead != -1) {
            readHeads.push(readHead) // save read head of previous component
        }
        
        // merge and push style
        val prevStyle = styles.top()
        val style = component.style().merge(prevStyle)
        styles.push(style)
        
        // push content
        val content = when (component) {
            is TextComponent -> component.content()
            is TranslatableComponent -> LocaleManager.getFormatString(lang, component.key())
            else -> throw UnsupportedOperationException("Unsupported component type: ${component::class.qualifiedName}")
        }
        contentStrings.push(content)
        
        // set current variables
        this.component = component
        this.style = style
        this.content = content
        this.readHead = 0
        
        // if the new component is empty, skip it
        if (content.isEmpty())
            nextComponent()
    }
    
}