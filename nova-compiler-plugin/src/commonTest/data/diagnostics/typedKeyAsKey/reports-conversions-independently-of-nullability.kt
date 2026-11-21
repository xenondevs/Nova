import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun accept(key: Key?) = Unit
fun returnKey(key: TypedKey<String>?): Key? = <!NOVA_TYPED_KEY_AS_KEY!>key<!>
fun test(key: TypedKey<String>, nullable: TypedKey<String>?) {
    val first: Key? = <!NOVA_TYPED_KEY_AS_KEY!>key<!>
    val second: Key? = <!NOVA_TYPED_KEY_AS_KEY!>nullable<!>
    accept(<!NOVA_TYPED_KEY_AS_KEY!>key<!>)
    accept(<!NOVA_TYPED_KEY_AS_KEY!>nullable<!>)
    <!NOVA_TYPED_KEY_AS_KEY!>nullable<!>?.namespace()
}
