import io.papermc.paper.registry.TypedKey

class Holder(val key: TypedKey<String>)

fun test(key: TypedKey<String>, holder: Holder) {
    <!NOVA_TYPED_KEY_AS_KEY!>key<!>.namespace()
    <!NOVA_TYPED_KEY_AS_KEY!>key<!>.value()
    <!NOVA_TYPED_KEY_AS_KEY!>key<!>.asString()
    <!NOVA_TYPED_KEY_AS_KEY!>holder.key<!>.asString()
}
