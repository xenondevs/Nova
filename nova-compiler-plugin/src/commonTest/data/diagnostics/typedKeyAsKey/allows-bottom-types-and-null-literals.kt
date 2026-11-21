import net.kyori.adventure.key.Key

fun accept(key: Key = error("missing")) = Unit
fun missing(): Key = error("missing")
fun thrown(): Key = throw IllegalStateException()
fun absent(): Key? = null
fun test(flag: Boolean, plain: Key) {
    val key: Key = error("missing")
    val nullable: Key? = null
    accept(error("missing"))
    val branch: Key = if (flag) error("missing") else plain
}