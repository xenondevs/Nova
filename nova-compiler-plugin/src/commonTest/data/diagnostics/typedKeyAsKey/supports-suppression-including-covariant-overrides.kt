import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

interface BaseHolder {
    val key: Key
}

interface Holder : BaseHolder {
    @Suppress("NOVA_TYPED_KEY_AS_KEY")
    override val key: TypedKey<String>
}

@Suppress("NOVA_TYPED_KEY_AS_KEY")
fun suppressed(key: TypedKey<String>): Key = key

fun test(holder: Holder) {
    holder.key.key().asString()
}