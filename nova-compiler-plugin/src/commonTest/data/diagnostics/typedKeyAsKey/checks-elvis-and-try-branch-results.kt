import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun test(typed: TypedKey<String>?, plain: Key): Key {
    val elvis: Key = <!NOVA_TYPED_KEY_AS_KEY!>typed<!> ?: plain
    return try {
        <!NOVA_TYPED_KEY_AS_KEY!>typed<!>
    } catch (exception: Exception) {
        plain
    } ?: plain
}
