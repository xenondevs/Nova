import net.kyori.adventure.key.Key

abstract class CustomKey : Key

fun direct(key: Key) = key.<!NOVA_KEY_TO_STRING!>toString<!>()
fun safe(key: Key?) = key?.<!NOVA_KEY_TO_STRING!>toString<!>()
fun nullable(key: Key?) = key.<!NOVA_KEY_TO_STRING!>toString<!>()
fun inferred() = Key.key("example:key").<!NOVA_KEY_TO_STRING!>toString<!>()
fun subtype(key: CustomKey) = key.<!NOVA_KEY_TO_STRING!>toString<!>()

class AttachmentType(val key: Key) {
    override fun toString(): String = this@AttachmentType.key.<!NOVA_KEY_TO_STRING!>toString<!>()
}
