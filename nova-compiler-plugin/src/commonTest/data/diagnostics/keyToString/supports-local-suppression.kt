import net.kyori.adventure.key.Key

@Suppress("NOVA_KEY_TO_STRING")
fun suppressed(key: Key) = key.toString()

fun reported(key: Key) = key.<!NOVA_KEY_TO_STRING!>toString<!>()
