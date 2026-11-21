import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

interface KeyOwner {
    val key: Key
}

class TypedKeyOwner(
    override val key: <!NOVA_TYPED_KEY_AS_KEY!>TypedKey<String><!>
) : KeyOwner

class TypedKeyOwnerBody(typedKey: TypedKey<String>) : KeyOwner {
    override val key: <!NOVA_TYPED_KEY_AS_KEY!>TypedKey<String><!> = typedKey
}
