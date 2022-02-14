package xyz.xenondevs.nova.ui.menu

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.builder.PotionBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.SimpleItem
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.ui.config.side.BackItem
import xyz.xenondevs.nova.ui.item.AioNumberItem
import xyz.xenondevs.nova.ui.overlay.CoreGUITexture
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
    
    private val gui = GUIBuilder(GUIType.NORMAL, 9, 3)
        .setStructure("" +
            "< . . . p . . . ." +
            ". . . . . . . . ." +
            ". . r . g . b . .")
        .addIngredient('p', colorPreviewItem)
        .addIngredient('r', ChangeColorItem({ red }, { red = it }, "menu.nova.color_picker.red", ItemBuilder(Material.RED_DYE)))
        .addIngredient('g', ChangeColorItem({ green }, { green = it }, "menu.nova.color_picker.green", ItemBuilder(Material.LIME_DYE)))
        .addIngredient('b', ChangeColorItem({ blue }, { blue = it }, "menu.nova.color_picker.blue", ItemBuilder(Material.BLUE_DYE)))
        .addIngredient('<', BackItem(openPrevious))
        .build()
    
    init {
        colorPreviewItem.color = color
    }
    
    private fun updateColorPreview() {
        colorPreviewItem.color = Color(red, green, blue)
    }
    
    fun openWindow(player: Player) {
        // TODO: change to correct gui texture when possible
        SimpleWindow(player, CoreGUITexture.EMPTY_GUI.getTitle("menu.nova.color_picker"), gui).show()
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

abstract class ColorPreviewItem(color: Color) : BaseItem() {
    
    var color: Color = color
        set(value) {
            field = value
            notifyWindows()
        }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
    
}

class PotionColorPreviewItem(builder: PotionBuilder, color: Color = Color(0, 0, 0)) : ColorPreviewItem(color) {
    
    private val builder: PotionBuilder = builder.clone()
    
    override fun getItemProvider(): PotionBuilder =
        builder.setColor(color)
    
}

class OpenColorPickerWindowItem(private val window: ColorPickerWindow) : SimpleItem(NovaMaterialRegistry.COLOR_PICKER_ICON.itemProvider) {
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        window.openWindow(player)
    }
}