import org.bukkit.Material

fun accept(value: Any) = Unit
fun test(block: org.bukkit.Block) {
    accept(<!NOVA_MATERIAL_USAGE!>Material.STONE<!>)
    <!NOVA_MATERIAL_USAGE!>block.type<!>.something
}

val Any.something get() = Unit
