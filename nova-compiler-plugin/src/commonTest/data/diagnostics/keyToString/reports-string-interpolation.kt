import net.kyori.adventure.key.Key

fun test(key: Key, nullable: Key?) {
    println("$<!NOVA_KEY_TO_STRING!>key<!>")
    println("${<!NOVA_KEY_TO_STRING!>key<!>}")
    println("$<!NOVA_KEY_TO_STRING!>nullable<!>")
}
