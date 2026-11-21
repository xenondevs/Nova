import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun accept(key: Key) = Unit
fun returnKey(key: TypedKey<String>): Key = <!NOVA_TYPED_KEY_AS_KEY!>key<!>
class Holder(val key: TypedKey<String>)

fun returnQualifiedKey(holder: Holder): Key = <!NOVA_TYPED_KEY_AS_KEY!>holder.key<!>

fun test(key: TypedKey<String>, holder: Holder) {
    var assigned: Key = <!NOVA_TYPED_KEY_AS_KEY!>key<!>
    assigned = <!NOVA_TYPED_KEY_AS_KEY!>holder.key<!>
    accept(<!NOVA_TYPED_KEY_AS_KEY!>key<!>)
    accept(key = <!NOVA_TYPED_KEY_AS_KEY!>holder.key<!>)
}
