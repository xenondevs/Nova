import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun test(flag: Boolean, typed: TypedKey<String>, plain: Key) {
    val assigned: Key = if (flag) {
        @Suppress("NOVA_TYPED_KEY_AS_KEY")
        typed
    } else {
        typed
        val explicit: Key = <!NOVA_TYPED_KEY_AS_KEY!>typed<!>
        plain
    }
    val returned: () -> Key = {
        if (flag) {
            @Suppress("NOVA_TYPED_KEY_AS_KEY")
            typed
        } else plain
    }
}
