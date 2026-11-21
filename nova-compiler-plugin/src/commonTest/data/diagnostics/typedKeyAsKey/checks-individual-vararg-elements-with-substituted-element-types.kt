import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun accept(vararg keys: Key) = Unit
fun <T> generic(vararg values: T) = Unit

fun test(typed: TypedKey<String>, plain: Key, keys: Array<Key>) {
    accept(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    accept(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>, plain, <!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    accept(*keys, <!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    generic<Key>(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    generic(plain, <!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    accept(plain, typed.key())
    accept(*keys)
    accept(keys = keys)
    generic(typed)
}
