package xyz.xenondevs.nova.world.item

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistrar.itemTag
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import xyz.xenondevs.nova.registry.registryEntrySetOf
import xyz.xenondevs.nova.world.item.behavior.Chargeable

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
object DefaultItemTags {
    
    /**
     * Contains all non-hidden Nova items.
     */
    val NOVA = itemTag("nova") {}
    
    /**
     * Contains all hidden Nova items.
     */
    val NOVA_HIDDEN = itemTag("nova_hidden") {}
    
    /**
     * Contains both [NOVA] and [NOVA_HIDDEN].
     */
    val NOVA_ALL = itemTag("nova_all") {
        add(NOVA)
        add(NOVA_HIDDEN)
    }
    
    /**
     * Contains all items with the [Chargeable] behavior.
     */
    val CHARGEABLE = itemTag("chargeable") {}
    
    init {
        // minecraft:shears tag for legacy tool behavior with "tool_category: shears"
        itemTag(registryEntrySetOf(TagKey.create(RegistryKey.ITEM, Key.key("minecraft", "shears")))) { 
            add(ItemTypeEntries.SHEARS) 
        }
    }
    
}