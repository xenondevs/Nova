import org.bukkit.Keyed
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.*

fun <P : Keyed> test(
    nullablePaperEntry: RegistryEntry.Paper<ItemType>?,
    paperValue: ItemType,
    novaEntry: RegistryEntry<NovaItem>,
    nullableNovaValue: NovaItem?,
    genericPaperEntry: RegistryEntry<P>,
    genericPaperValue: P,
) {
    when (nullablePaperEntry) {
        <!NOVA_REGISTRY_ENTRY_COMPARISON!>paperValue<!> -> Unit
        else -> Unit
    }
    when (nullableNovaValue) {
        <!NOVA_REGISTRY_ENTRY_COMPARISON!>novaEntry<!> -> Unit
        else -> Unit
    }
    when (genericPaperValue) {
        <!NOVA_REGISTRY_ENTRY_COMPARISON!>genericPaperEntry<!> -> Unit
        else -> Unit
    }
}
