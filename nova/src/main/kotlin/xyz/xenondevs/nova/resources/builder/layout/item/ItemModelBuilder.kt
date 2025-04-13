@file:OptIn(InternalResourcePackDTO::class)

package xyz.xenondevs.nova.resources.builder.layout.item

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.InternalResourcePackDTO
import xyz.xenondevs.nova.resources.builder.data.ItemModel
import xyz.xenondevs.nova.resources.builder.data.TintSource
import xyz.xenondevs.nova.resources.builder.layout.ModelSelectorScope
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import java.awt.Color

@RegistryElementBuilderDsl
class ItemModelBuilder<S : ModelSelectorScope> internal constructor(
    val resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) {
    
    /**
     * A function to select the corresponding [ModelBuilder].
     */
    var model: S.() -> ModelBuilder = { defaultModel }
    
    /**
     * A map of `tintindex` to the corresponding [TintSource].
     */
    var tintSource: MutableMap<Int, TintSource> = Int2ObjectOpenHashMap()
    
    internal fun build(): ItemModel.Default {
        val highestTintSourceId = tintSource.keys.maxOrNull()
        if (highestTintSourceId != null) {
            val tintSourcesList = Array(highestTintSourceId + 1) { tintSource[it] ?: TintSource.Constant(Color.WHITE) }.asList()
            return ItemModel.Default(selectAndBuild(model), tintSourcesList)
        } else {
            return ItemModel.Default(selectAndBuild(model))
        }
    }
    
}