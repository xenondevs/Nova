import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey

fun test(key: Key, named: NamespacedKey, nullable: NamespacedKey?, text: String) {
    key.asString()
    text.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString()<!>
    named.toString()
    nullable.toString()
    nullable?.toString()
    println("$named $nullable $text")
}
