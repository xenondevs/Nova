@file:Suppress("NOVA_REGISTRY_ENTRY_COMPARISON")

import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.*

fun test(entry: RegistryEntry<ItemType>, value: ItemType) = entry == value
