import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

class Box<T>(val value: T) {
    fun consume(value: T) = Unit
}

interface Consumer<T> {
    fun consume(value: T)
}

interface Keys : Consumer<Key>

fun <T> List<T>.consume(value: T) = Unit

fun test(typed: TypedKey<String>, map: MutableMap<Key, String>, box: Box<Key>, keys: Keys) {
    map[<!NOVA_TYPED_KEY_AS_KEY!>typed<!>] = "value"
    box.consume(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    keys.consume(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    Box<Key>(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    emptyList<Key>().consume(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
}
