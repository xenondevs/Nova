import org.bukkit.Keyed
import xyz.xenondevs.nova.registry.*

fun <N : NovaRegistryElement<N>, P : Keyed> test(
    novaEntry: RegistryEntry<N>,
    nullableNovaEntrySubtype: RegistryEntry.Nova<N>?,
    nullableNovaValue: N?,
    paperEntry: RegistryEntry.Paper<P>,
    paperValue: P,
) {
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>novaEntry == nullableNovaValue<!>
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>nullableNovaValue != nullableNovaEntrySubtype<!>
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>paperEntry == paperValue<!>
}
