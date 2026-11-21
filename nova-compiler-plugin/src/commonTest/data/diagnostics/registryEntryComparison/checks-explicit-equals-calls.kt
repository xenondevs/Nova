import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.*

fun test(entry: RegistryEntry<ItemType>, value: ItemType, nullable: RegistryEntry<ItemType>?, other: RegistryEntry<ItemType>) {
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>entry.equals(value)<!>
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>value.equals(entry)<!>
    <!NOVA_REGISTRY_ENTRY_COMPARISON!>nullable?.equals(value)<!>
    entry.equals(other)
}
