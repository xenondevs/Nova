import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

class Wrapped(typed: TypedKey<String>) : Key by <!NOVA_TYPED_KEY_AS_KEY!>typed<!>
class Conditional(flag: Boolean, typed: TypedKey<String>, plain: Key) :
    Key by (if (flag) <!NOVA_TYPED_KEY_AS_KEY!>typed<!> else plain)

class Explicit(typed: TypedKey<String>) : Key by typed.key()

@Suppress(<!ERROR_SUPPRESSION!>"CLASS_INHERITS_JAVA_SEALED_CLASS"<!>)
class Typed(typed: TypedKey<String>) : TypedKey<String> by typed
class Plain(key: Key) : Key by key

fun anonymous(typed: TypedKey<String>) = object : Key by <!NOVA_TYPED_KEY_AS_KEY!>typed<!> {}
