package xyz.xenondevs.nova.ui.menu

import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.dsl.DslProperty
import xyz.xenondevs.invui.dsl.WindowDsl
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.overlay.guitexture.getTitle
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.clientsideProvider

@JvmName("itemProviderByNovaItemProvider")
infix fun DslProperty<in ItemProvider>.by(novaItem: Provider<NovaItem>): Unit =
    by(novaItem.clientsideProvider)

@JvmName("itemStackByItemProvider")
infix fun DslProperty<in ItemStack>.by(itemProvider: ItemProvider): Unit =
    by(itemProvider.get())

@JvmName("itemStackByItemProviderProvider")
infix fun DslProperty<in ItemStack>.by(itemProvider: Provider<ItemProvider>): Unit =
    by(itemProvider.map(ItemProvider::get))

@JvmName("componentByGuiTextureEntry")
context(dsl: WindowDsl)
infix fun DslProperty<Component>.by(guiTexture: RegistryEntry.Nova<GuiTexture>): Unit =
    by(guiTexture.getTitle(dsl.locale))