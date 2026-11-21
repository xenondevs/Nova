import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.*

fun test(
    novaEntry: RegistryEntry<NovaItem>,
    nullableNovaEntrySubtype: RegistryEntry.Nova<NovaItem>?,
    novaValue: NovaItem,
    nullableNovaValue: NovaItem?,
    nullablePaperEntry: RegistryEntry<ItemType>?,
    paperEntrySubtype: RegistryEntry.Paper<ItemType>,
    paperValue: ItemType,
    nullablePaperValue: ItemType?,
) {
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>novaEntry == novaValue<!>
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>nullableNovaValue != novaEntry<!>
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>nullableNovaEntrySubtype == novaValue<!>
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>nullablePaperEntry != nullablePaperValue<!>
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>paperValue == paperEntrySubtype<!>
}
