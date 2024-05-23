package xyz.xenondevs.nova.ui.menu

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.type.context.setTitle
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.ui.menu.item.AioNumberItem
import xyz.xenondevs.nova.ui.menu.item.BackItem
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import java.awt.Color

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
    
    private val gui = Gui.normal()
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
        Window.single { 
            it.setViewer(player)
            it.setTitle(DefaultGuiTextures.COLOR_PICKER.getTitle("menu.nova.color_picker"))
            it.setGui(gui)
        }.open()
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

abstract class ColorPreviewItem(color: Color) : AbstractItem() {
    
    var color: Color = color
        set(value) {
            field = value
            notifyWindows()
        }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
    
}

class OpenColorPickerWindowItem(private val window: ColorPickerWindow) : SimpleItem(DefaultGuiItems.TP_COLOR_PICKER.model.clientsideProvider) {
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        window.openWindow(player)
    }
}