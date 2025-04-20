package xyz.xenondevs.nova.ui.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.ui.menu.item.AioNumberItem
import xyz.xenondevs.nova.ui.menu.item.BackItem
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import java.awt.Color

@Deprecated("Color picker will be removed in a future version")
class ColorPickerWindow(
    private val colorPreviewItem: ColorPreviewItem,
    color: Color,
    openPrevious: (Player) -> Unit
) {
    
    val color: Color
        get() = Color(red, green, blue)
    
    private var red = color.red
        set(value) {
            field = value
            updateColorPreview()
        }
    
    private var green = color.green
        set(value) {
            field = value
            updateColorPreview()
        }
    
    private var blue = color.blue
        set(value) {
            field = value
            updateColorPreview()
        }
    
    private val gui = Gui.builder()
        .setStructure(
            "< . . . p . . . .",
            ". . . . . . . . .",
            ". . r . g . b . ."
        )
        .addIngredient('p', colorPreviewItem)
        .addIngredient('r', ChangeColorItem({ red }, { red = it }, "menu.nova.color_picker.red", ItemBuilder(Material.RED_DYE)))
        .addIngredient('g', ChangeColorItem({ green }, { green = it }, "menu.nova.color_picker.green", ItemBuilder(Material.LIME_DYE)))
        .addIngredient('b', ChangeColorItem({ blue }, { blue = it }, "menu.nova.color_picker.blue", ItemBuilder(Material.BLUE_DYE)))
        .addIngredient('<', BackItem(openPrevious = openPrevious))
        .build()
    
    init {
        colorPreviewItem.color = color
    }
    
    private fun updateColorPreview() {
        colorPreviewItem.color = Color(red, green, blue)
    }
    
    fun openWindow(player: Player) {
        Window.builder()
            .setTitle(DefaultGuiTextures.COLOR_PICKER.getTitle("menu.nova.color_picker"))
            .setUpperGui(gui)
            .open(player)
    }
    
}

private class ChangeColorItem(
    getNumber: () -> Int,
    setNumber: (Int) -> Unit,
    localizedName: String,
    builder: ItemBuilder
) : AioNumberItem(
    1, 10,
    { 0..255 },
    getNumber, setNumber,
    localizedName, builder
)

@Deprecated("Color picker will be removed in a future version")
abstract class ColorPreviewItem(color: Color) : AbstractItem() {
    
    var color: Color = color
        set(value) {
            field = value
            notifyWindows()
        }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) = Unit
    
}

@Deprecated("Color picker will be removed in a future version")
class OpenColorPickerWindowItem(private val window: ColorPickerWindow) : AbstractItem() {
    
    override fun getItemProvider(player: Player) = DefaultGuiItems.TP_COLOR_PICKER.clientsideProvider
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        window.openWindow(player)
    }
    
}