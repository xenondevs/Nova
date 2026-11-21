import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key

fun accept(key: Key) = Unit
fun acceptTyped(key: TypedKey<String>) = Unit

fun test(flag: Boolean, key: TypedKey<String>, plain: Key) {
    acceptTyped(key)
    accept(key.key())
    key.key().namespace()
    key.registryKey()
    val any: Any = key
    val branch: Any = if (flag) key else plain
    val typed: TypedKey<String> = if (flag) key else key
    val inferred = key
}