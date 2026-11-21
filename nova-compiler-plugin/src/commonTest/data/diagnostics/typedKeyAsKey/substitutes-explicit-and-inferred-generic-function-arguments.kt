import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun <T> consume(value: T) = Unit
fun <T> choose(first: T, second: T): T = first

fun test(typed: TypedKey<String>, plain: Key) {
    consume<Key>(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    consume<Key?>(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    choose(plain, <!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    consume(typed)
    consume<Any>(typed)
    consume<TypedKey<String>>(typed)
}
