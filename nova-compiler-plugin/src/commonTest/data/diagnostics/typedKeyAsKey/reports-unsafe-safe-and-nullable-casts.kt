import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun test(key: TypedKey<String>, nullable: TypedKey<String>?) {
    <!NOVA_TYPED_KEY_AS_KEY!>key<!> as Key
    <!NOVA_TYPED_KEY_AS_KEY!>key<!> as? Key
    <!NOVA_TYPED_KEY_AS_KEY!>key<!> as Key?
    <!NOVA_TYPED_KEY_AS_KEY!>nullable<!> as Key?
}
