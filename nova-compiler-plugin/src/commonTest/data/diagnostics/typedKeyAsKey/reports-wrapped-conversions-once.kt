import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

class Holder(val typed: TypedKey<String>)

fun accept(key: Key?) = Unit
fun test(flag: Boolean, typed: TypedKey<String>?, holder: Holder?) {
    accept(<!NOVA_TYPED_KEY_AS_KEY!>holder?.typed<!>)
    if (typed != null) {
        accept(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
    }
    accept(<!NOVA_TYPED_KEY_AS_KEY!>if (flag) holder?.typed else typed<!>)
    val key: Key? = <!NOVA_TYPED_KEY_AS_KEY!>run { holder?.typed }<!>
}
