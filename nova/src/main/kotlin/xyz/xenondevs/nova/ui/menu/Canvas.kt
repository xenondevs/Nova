package xyz.xenondevs.nova.ui.menu

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomModelData
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.notifyWindows
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelCreationScope
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.NovaItem
import java.awt.image.BufferedImage
import java.util.function.Supplier

/**
 * An [Item] supplier for the [canvasItem] that splits [image] into square parts of [itemResolution]x[itemResolution] px.
 * 
 * @param canvasItem The canvas item, which uses a model created via [ItemModelCreationScope.canvasModel].
 * This model should be square and should completely fill an entire slot (18x18 px or multiples of that at non-1 scales).
 * @param itemResolution The size of the canvas item in pixels (18 px or multiples of that at non-1 scales).
 * @param image The image that is used to fill the canvas. Will be read from every time [notifyWindows] is called.
 * 
 * @see ItemModelCreationScope.canvasModel
 */
open class Canvas(
    private val canvasItem: NovaItem,
    private val itemResolution: Int,
    private val image: BufferedImage
) : Supplier<Item> {
    
    private val items = ArrayList<Item>()
    private var supplierIndex = 0
    
    /**
     * Creates a new [Canvas] with the [DefaultGuiItems.CANVAS] (18x18 px) item.
     * 
     * @param image The image that is used to fill the canvas. Will be read from every time [notifyWindows] is called.
     */
    constructor(image: BufferedImage) : this(DefaultGuiItems.CANVAS, 18, image)
    
    init {
        require(image.height % itemResolution == 0) { "Image height needs to be divisible by $itemResolution" }
        require(image.width % itemResolution == 0) { "Image width needs to be divisible by $itemResolution" }
        
        for (y in 0..<(image.height / itemResolution)) {
            for (x in 0..<(image.width / itemResolution)) {
                items += CanvasPart(x, y)
            }
        }
    }
    
    override fun get(): Item {
        return items[supplierIndex++]
    }
    
    /**
     * [Notifies][Item.notifyWindows] all windows of all items of this canvas.
     */
    fun notifyWindows() {
        items.notifyWindows()
    }
    
    /**
     * Modifies the [itemBuilder] for the canvas part at the given [x] and [y] coordinates,
     * which will be displayed to [viewer].
     */
    protected open fun modifyItemBuilder(x: Int, y: Int, viewer: Player, itemBuilder: ItemBuilder) = Unit
    
    /**
     * Handles a [click] on the canvas part at the given [x] and [y] coordinates.
     */
    protected open fun handleClick(x: Int, y: Int, click: Click) = Unit
    
    private inner class CanvasPart(private val x: Int, private val y: Int) : AbstractItem() {
        
        private val colors = IntArray(itemResolution * itemResolution)
        
        override fun getItemProvider(viewer: Player): ItemProvider {
            // read colors from image
            image.getRGB(
                x * itemResolution, y * itemResolution,
                itemResolution, itemResolution,
                colors,
                0,
                itemResolution
            )
            
            // write colors to item stack
            val itemStack = canvasItem.clientsideProvider.get().unwrap().copy()
            itemStack.set(
                DataComponents.CUSTOM_MODEL_DATA,
                CustomModelData(
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    IntArrayList(colors)
                )
            )
            
            val builder = ItemBuilder(itemStack.asBukkitMirror())
            modifyItemBuilder(x, y, viewer, builder)
            return builder
        }
        
        override fun handleClick(clickType: ClickType, player: Player, click: Click) {
            handleClick(x, y, click)
        }
        
    }
    
}