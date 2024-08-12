package xyz.xenondevs.nova.ui.menu.item

import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.AbstractItem
import java.util.concurrent.atomic.AtomicReference

internal abstract class AsyncItem(default: ItemProvider = ItemProvider.EMPTY) : AbstractItem() {
    
    protected val provider: AtomicReference<ItemProvider> = AtomicReference(default)
    
    final override fun getItemProvider(): ItemProvider = provider.get()
    
    abstract fun updateAsync()
    
}