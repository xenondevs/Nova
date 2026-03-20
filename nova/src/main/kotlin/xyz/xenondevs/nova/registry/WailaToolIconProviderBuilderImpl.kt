package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier

internal class WailaToolIconProviderBuilderImpl(
    override val entry: RegistryEntry.Nova<WailaToolIconProvider>
) : WailaToolIconProviderBuilder, RegistryElementBuilder.Nova<WailaToolIconProvider> {
    
    private var iconGetter: (category: RegistryEntry.Nova<ToolCategory>, tier: RegistryEntry.Nova<ToolTier>?) -> Key? = { _, _ -> null }
    
    override fun iconProvider(iconGetter: (category: RegistryEntry.Nova<ToolCategory>, tier: RegistryEntry.Nova<ToolTier>?) -> Key?) {
        this.iconGetter = iconGetter
    }
    
    override fun build() = WailaToolIconProvider(entry, iconGetter)
    
}