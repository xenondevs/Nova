import org.bukkit.Material
import org.bukkit.inventory.ItemType

val materialClass = <!NOVA_MATERIAL_USAGE!>Material::class<!>
fun materialClass() = <!NOVA_MATERIAL_USAGE!>Material::class<!>
val itemClass = ItemType::class

@Suppress("NOVA_MATERIAL_USAGE")
val suppressed = Material::class
