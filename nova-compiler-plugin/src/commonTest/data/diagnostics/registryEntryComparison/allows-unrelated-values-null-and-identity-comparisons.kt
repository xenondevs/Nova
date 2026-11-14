import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.*

fun test(entry: RegistryEntry<ItemType>, value: ItemType, unrelated: NovaItem) {
    entry == unrelated
    <!SENSELESS_COMPARISON!>entry == null<!>
    entry === value
    entry !== value
}
