import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun makeTypedKey(): TypedKey<String> = TODO()
fun accept(key: Key = <!NOVA_TYPED_KEY_AS_KEY!>makeTypedKey()<!>) = Unit
fun nullable(key: Key? = <!NOVA_TYPED_KEY_AS_KEY!>makeTypedKey()<!>) = Unit
class Holder(val key: Key = <!NOVA_TYPED_KEY_AS_KEY!>makeTypedKey()<!>)
fun branches(flag: Boolean, plain: Key, key: Key = if (flag) <!NOVA_TYPED_KEY_AS_KEY!>makeTypedKey()<!> else plain) = Unit
fun allowed(key: TypedKey<String> = makeTypedKey()) = Unit
