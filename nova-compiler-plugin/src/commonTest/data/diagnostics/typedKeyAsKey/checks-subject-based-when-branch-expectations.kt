import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun test(key: Key, nullable: Key?, typed: TypedKey<String>, any: Any) {
    when (key) {
        <!NOVA_TYPED_KEY_AS_KEY!>typed<!> -> Unit
        else -> Unit
    }
    when (nullable) {
        null -> Unit
        <!NOVA_TYPED_KEY_AS_KEY!>typed<!> -> Unit
        else -> Unit
    }
    when (typed) {
        key -> Unit
        else -> Unit
    }
    when (any) {
        typed -> Unit
        else -> Unit
    }
    when {
        typed == key -> Unit
        else -> Unit
    }
}
