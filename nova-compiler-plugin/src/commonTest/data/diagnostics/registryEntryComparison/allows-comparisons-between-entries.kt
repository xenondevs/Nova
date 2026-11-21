import org.bukkit.Keyed
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.*

fun <N : NovaRegistryElement<N>, P : Keyed> test(
    novaEntry: RegistryEntry<NovaItem>,
    nullableNovaEntrySubtype: RegistryEntry.Nova<NovaItem>?,
    paperEntrySubtype: RegistryEntry.Paper<ItemType>,
    nullablePaperEntry: RegistryEntry<ItemType>?,
    genericNovaEntry: RegistryEntry<N>,
    nullableGenericNovaEntrySubtype: RegistryEntry.Nova<N>?,
    nullableGenericPaperEntrySubtype: RegistryEntry.Paper<P>?,
    genericPaperEntry: RegistryEntry<P>,
) {
    novaEntry == nullableNovaEntrySubtype
    paperEntrySubtype != nullablePaperEntry
    genericNovaEntry == nullableGenericNovaEntrySubtype
    nullableGenericPaperEntrySubtype != genericPaperEntry
    when (nullablePaperEntry) {
        paperEntrySubtype -> Unit
        else -> Unit
    }
    when (genericNovaEntry) {
        nullableGenericNovaEntrySubtype -> Unit
        else -> Unit
    }
}
