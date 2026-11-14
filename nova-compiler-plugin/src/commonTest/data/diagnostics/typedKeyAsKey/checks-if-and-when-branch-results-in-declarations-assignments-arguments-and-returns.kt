import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun accept(key: Key) = Unit
fun returned(flag: Boolean, typed: TypedKey<String>, plain: Key): Key =
    if (flag) <!NOVA_TYPED_KEY_AS_KEY!>typed<!> else plain

fun test(flag: Boolean, typed: TypedKey<String>, plain: Key) {
    var key: Key = if (flag) <!NOVA_TYPED_KEY_AS_KEY!>typed<!> else plain
    key = if (flag) plain else <!NOVA_TYPED_KEY_AS_KEY!>typed<!>
    accept(if (flag) <!NOVA_TYPED_KEY_AS_KEY!>typed<!> else plain)
    val nested: Key = when {
        flag -> if (flag) <!NOVA_TYPED_KEY_AS_KEY!>typed<!> else plain
        else -> {
            println("branch")
            <!NOVA_TYPED_KEY_AS_KEY!>typed<!>
        }
    }
    accept(<!NOVA_TYPED_KEY_AS_KEY!>if (flag) typed else typed<!>)
}
