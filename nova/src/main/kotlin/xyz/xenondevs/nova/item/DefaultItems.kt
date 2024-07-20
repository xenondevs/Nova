package xyz.xenondevs.nova.item

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.item.behavior.UnknownItemFilterBehavior

@InternalInit(stage = InternalInitStage.PRE_WORLD)
internal object DefaultItems {
    
    val UNKNOWN_ITEM_FILTER = item("unknown_item_filter") {
        behaviors(UnknownItemFilterBehavior)
        models { selectModel { getModel("block/unknown") } }
    }
    
    private fun item(name: String, run: NovaItemBuilder.() -> Unit): NovaItem {
        val builder = NovaItemBuilder(ResourceLocation.fromNamespaceAndPath("nova", name))
        builder.run()
        return builder.register()
    }
    
}