package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.ItemProvider
import java.util.concurrent.atomic.AtomicReference

internal abstract class AsyncItem(default: ItemProvider = ItemProvider.EMPTY) : AbstractItem() {
    
    protected val provider: AtomicReference<ItemProvider> = AtomicReference(default)
    
    final override fun getItemProvider(player: Player): ItemProvider = provider.get()
    
    abstract fun updateAsync()
    
}