package xyz.xenondevs.nova.integration.customitems.plugin

import dev.lone.itemsadder.api.CustomBlock
import dev.lone.itemsadder.api.CustomStack
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent.Cause
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.util.playPlaceSoundEffect
import xyz.xenondevs.nova.util.runAsyncTask

object ItemsAdder : CustomItemService {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null
    
    init {
        if (isInstalled) {
            Bukkit.getPluginManager().registerEvents(
                ItemsAdderLoadListener {
                    CustomItemServiceManager.READY_LATCH.countDown()
                },
                NOVA
            )
        }
    }
    
    override fun breakBlock(block: Block, tool: ItemStack?): List<ItemStack>? {
        val customBlock = CustomBlock.byAlreadyPlaced(block)
        if (customBlock != null) {
            val loot = customBlock.getLoot(true)
            customBlock.remove()
            return loot
        }
        
        return null
    }
    
    override fun placeItem(item: ItemStack, location: Location): Boolean {
        // Note: CustomBlock.byItemStack(item) can't be used because of an illegal cast in the ItemsAdder API
        val customItem = CustomStack.byItemStack(item)
        if (customItem == null || !customItem.isBlock)
            return false
        CustomBlock.place(customItem.namespacedID, location)
        Material.STONE.playPlaceSoundEffect(location)
        return true
    }
    
    override fun getItemByName(name: String): ItemStack? {
        val customItem = CustomStack.getInstance(name)
        return customItem?.itemStack
    }
    
    override fun hasNamespace(namespace: String): Boolean {
        // TODO: Not in the ItemsAdder API yet
        return namespace == "itemsadder"
    }
    
    override fun getNameKey(item: ItemStack): String? {
        return CustomStack.byItemStack(item)?.namespacedID
    }
    
}

class ItemsAdderLoadListener(private val run: () -> Unit) : Listener {
    
    @EventHandler
    fun handleItemsAdderLoadData(event: ItemsAdderLoadDataEvent) {
        if (event.cause == Cause.FIRST_LOAD) {
            runAsyncTask {
                Thread.sleep(10_000) // it is a lie, ItemsAdder isn't done yet
                run()
            }
        } else {
            NOVA.logger.warning("Reloading ItemsAdder might cause issues when items from ItemsAdder are used in Nova recipes.")
        }
    }
    
}