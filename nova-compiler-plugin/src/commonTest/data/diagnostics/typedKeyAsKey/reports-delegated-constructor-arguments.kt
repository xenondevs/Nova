import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

open class Base(key: Key)
class Primary(typed: TypedKey<String>) : Base(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
class Secondary : Base {
    constructor(typed: TypedKey<String>) : super(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
}

class Chained(key: Key) {
    constructor(typed: TypedKey<String>, marker: Int) : this(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
}

open class GenericBase<T>(key: T)
class Generic(typed: TypedKey<String>) : GenericBase<Key>(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>)
open class VarargBase(vararg keys: Key)
class Vararg(typed: TypedKey<String>) : VarargBase(<!NOVA_TYPED_KEY_AS_KEY!>typed<!>, <!NOVA_TYPED_KEY_AS_KEY!>typed<!>)

class Explicit(typed: TypedKey<String>) : Base(typed.key())
class Typed(typed: TypedKey<String>) : GenericBase<TypedKey<String>>(typed)
