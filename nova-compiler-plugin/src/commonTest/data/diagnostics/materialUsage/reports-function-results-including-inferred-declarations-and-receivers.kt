import org.bukkit.Material
import org.bukkit.getMaterial

fun test() {
    <!NOVA_MATERIAL_USAGE!>Material.valueOf("STONE")<!>
    <!NOVA_MATERIAL_USAGE!>getMaterial()<!>.name
    val inferred = <!NOVA_MATERIAL_USAGE!>getMaterial()<!>
    val nested = <!NOVA_MATERIAL_USAGE!>listOf(<!NOVA_MATERIAL_USAGE!>getMaterial()<!>)<!>
}
