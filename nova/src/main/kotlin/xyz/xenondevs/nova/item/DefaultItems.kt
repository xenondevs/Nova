package xyz.xenondevs.nova.item

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.layout.item.ItemModelLayoutBuilder
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.logic.PacketItems

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object DefaultGuiItems {
    
    // GUI items with custom model
    val ANVIL_OVERLAY_ARROW = hiddenItem("gui/anvil_overlay_arrow")
    val ANVIL_OVERLAY_ARROW_LEFT = hiddenItem("gui/anvil_overlay_arrow_left")
    val ANVIL_OVERLAY_PLUS = hiddenItem("gui/anvil_overlay_plus")
    
    // GUI items without custom model
    
    //<editor-fold desc="with background">
    // legacy InvUI gui items
    val INVENTORY_PART = guiItem("inventory_part")
    val INVISIBLE_ITEM = hiddenItem("empty")
    val LINE_CORNER_BOTTOM_LEFT = guiItem("line/corner_bottom_left", stretched = true)
    val LINE_CORNER_BOTTOM_RIGHT = guiItem("line/corner_bottom_right", stretched = true)
    val LINE_CORNER_TOP_LEFT = guiItem("line/corner_top_left", stretched = true)
    val LINE_CORNER_TOP_RIGHT = guiItem("line/corner_top_right", stretched = true)
    val LINE_HORIZONTAL_DOWN = guiItem("line/horizontal_down", stretched = true)
    val LINE_HORIZONTAL = guiItem("line/horizontal", stretched = true)
    val LINE_HORIZONTAL_UP = guiItem("line/horizontal_up", stretched = true)
    val LINE_VERTICAL_HORIZONTAL = guiItem("line/vertical_horizontal", stretched = true)
    val LINE_VERTICAL_LEFT = guiItem("line/vertical_left", stretched = true)
    val LINE_VERTICAL = guiItem("line/vertical", stretched = true)
    val LINE_VERTICAL_RIGHT = guiItem("line/vertical_right", stretched = true)
    
    // buttons
    val AREA_BTN_OFF = guiItem("btn/area_off", "menu.nova.visual_region.show")
    val AREA_BTN_ON = guiItem("btn/area_on", "menu.nova.visual_region.hide")
    val ARROW_LEFT_BTN_OFF = guiItem("btn/arrow_left_off")
    val ARROW_LEFT_BTN_ON = guiItem("btn/arrow_left_on")
    val ARROW_RIGHT_BTN_OFF = guiItem("btn/arrow_right_off")
    val ARROW_RIGHT_BTN_ON = guiItem("btn/arrow_left_on")
    val MINUS_BTN_OFF = guiItem("btn/minus_off")
    val MINUS_BTN_ON = guiItem("btn/minus_on")
    val PLUS_BTN_OFF = guiItem("btn/plus_off")
    val PLUS_BTN_ON = guiItem("btn/plus_on")
    val SIDE_CONFIG_BTN = guiItem("btn/side_config", "menu.nova.side_config")
    val ENERGY_BTN_OFF = guiItem("btn/energy_off", "menu.nova.side_config.energy")
    val ENERGY_BTN_ON = guiItem("btn/energy_on", "menu.nova.side_config.energy")
    val ENERGY_BTN_SELECTED = guiItem("btn/energy_selected", "menu.nova.side_config.energy")
    val ITEM_BTN_OFF = guiItem("btn/items_off", "menu.nova.side_config.items")
    val ITEM_BTN_ON = guiItem("btn/items_on", "menu.nova.side_config.items")
    val ITEM_BTN_SELECTED = guiItem("btn/items_selected", "menu.nova.side_config.items")
    val FLUID_BTN_OFF = guiItem("btn/fluids_off", "menu.nova.side_config.fluids")
    val FLUID_BTN_ON = guiItem("btn/fluids_on", "menu.nova.side_config.fluids")
    val FLUID_BTN_SELECTED = guiItem("btn/fluids_selected", "menu.nova.side_config.fluids")
    val SIMPLE_MODE_BTN_OFF = guiItem("btn/simple_mode_off", "menu.nova.side_config.simple_mode")
    val SIMPLE_MODE_BTN_ON = guiItem("btn/simple_mode_on", "menu.nova.side_config.simple_mode")
    val ADVANCED_MODE_BTN_OFF = guiItem("btn/advanced_mode_off", "menu.nova.side_config.advanced_mode")
    val ADVANCED_MODE_BTN_ON = guiItem("btn/advanced_mode_on", "menu.nova.side_config.advanced_mode")
    val BLUE_BTN = guiItem("btn/blue")
    val GRAY_BTN = guiItem("btn/gray")
    val GREEN_BTN = guiItem("btn/green")
    val ORANGE_BTN = guiItem("btn/orange")
    val PINK_BTN = guiItem("btn/pink")
    val RED_BTN = guiItem("btn/red")
    val WHITE_BTN = guiItem("btn/white")
    val YELLOW_BTN = guiItem("btn/yellow")
    
    // other
    val ARROW_LEFT_OFF = guiItem("arrow/left_off")
    val ARROW_LEFT_ON = guiItem("arrow/left_on")
    val ARROW_RIGHT_OFF = guiItem("arrow/right_off")
    val ARROW_RIGHT_ON = guiItem("arrow/right_on")
    val ARROW_DOWN_OFF = guiItem("arrow/down_off")
    val ARROW_DOWN_ON = guiItem("arrow/down_on")
    val ARROW_UP_OFF = guiItem("arrow/up_off")
    val ARROW_UP_ON = guiItem("arrow/up_on")
    val SMALL_ARROW_LEFT_OFF = guiItem("small_arrow/left_off")
    val SMALL_ARROW_LEFT_ON = guiItem("small_arrow/left_on")
    val SMALL_ARROW_RIGHT_OFF = guiItem("small_arrow/right_off")
    val SMALL_ARROW_RIGHT_ON = guiItem("small_arrow/right_on")
    val SMALL_ARROW_DOWN_OFF = guiItem("small_arrow/down_off")
    val SMALL_ARROW_DOWN_ON = guiItem("small_arrow/down_on")
    val SMALL_ARROW_UP_OFF = guiItem("small_arrow/up_off")
    val SMALL_ARROW_UP_ON = guiItem("small_arrow/up_on")
    val BAR_BLUE = hiddenItem("gui/opaque/bar/blue") { selectModels(0..16, true) { createGuiModel("gui/bar/blue/$it", background = true, stretched = true) } }
    val BAR_GREEN = hiddenItem("gui/opaque/bar/green") { selectModels(0..16, true) { createGuiModel("gui/bar/green/$it", background = true, stretched = true) } }
    val BAR_RED = hiddenItem("gui/opaque/bar/red") { selectModels(0..16, true) { createGuiModel("gui/bar/red/$it", background = true, stretched = true) } }
    val BAR_ORANGE = hiddenItem("gui/opaque/bar/orange") { selectModels(0..16, true) { createGuiModel("gui/bar/orange/$it", background = true, stretched = true) } }
    val CHEATING_ON = guiItem("cheating_on", "menu.nova.items.cheat_mode_item")
    val CHEATING_OFF = guiItem("cheating_off", "menu.nova.items.cheat_mode_item")
    val COLOR_PICKER = guiItem("color_picker")
    val NUMBER = hiddenItem("gui/opaque/number") { selectModels(0..999, true) { createGuiModel("gui/number/$it", background = true, stretched = false) } }
    val SEARCH = guiItem("search")
    val STOPWATCH = guiItem("stopwatch")
    //</editor-fold>
    
    //<editor-fold desc="without background">
    // legacy InvUI gui items
    val TP_LINE_CORNER_BOTTOM_LEFT = tpGuiItem("line/corner_bottom_left", stretched = true)
    val TP_LINE_CORNER_BOTTOM_RIGHT = tpGuiItem("line/corner_bottom_right", stretched = true)
    val TP_LINE_CORNER_TOP_LEFT = tpGuiItem("line/corner_top_left", stretched = true)
    val TP_LINE_CORNER_TOP_RIGHT = tpGuiItem("line/corner_top_right", stretched = true)
    val TP_LINE_HORIZONTAL = tpGuiItem("line/horizontal", stretched = true)
    val TP_LINE_HORIZONTAL_DOWN = tpGuiItem("line/horizontal_down", stretched = true)
    val TP_LINE_HORIZONTAL_UP = tpGuiItem("line/horizontal_up", stretched = true)
    val TP_LINE_VERTICAL_HORIZONTAL = tpGuiItem("line/vertical_horizontal", stretched = true)
    val TP_LINE_VERTICAL = tpGuiItem("line/vertical", stretched = true)
    val TP_LINE_VERTICAL_LEFT = tpGuiItem("line/vertical_left", stretched = true)
    val TP_LINE_VERTICAL_RIGHT = tpGuiItem("line/vertical_right", stretched = true)
    
    // buttons
    val TP_AREA_BTN_OFF = tpGuiItem("btn/area_off", "menu.nova.visual_region.show")
    val TP_AREA_BTN_ON = tpGuiItem("btn/area_on", "menu.nova.visual_region.hide")
    val TP_ARROW_LEFT_BTN_OFF = tpGuiItem("btn/arrow_left_off")
    val TP_ARROW_LEFT_BTN_ON = tpGuiItem("btn/arrow_left_on")
    val TP_ARROW_RIGHT_BTN_OFF = tpGuiItem("btn/arrow_right_off")
    val TP_ARROW_RIGHT_BTN_ON = tpGuiItem("btn/arrow_left_on")
    val TP_MINUS_BTN_OFF = tpGuiItem("btn/minus_off")
    val TP_MINUS_BTN_ON = tpGuiItem("btn/minus_on")
    val TP_PLUS_BTN_OFF = tpGuiItem("btn/plus_off")
    val TP_PLUS_BTN_ON = tpGuiItem("btn/plus_on")
    val TP_SIDE_CONFIG_BTN = tpGuiItem("btn/side_config", "menu.nova.side_config")
    val TP_ENERGY_BTN_OFF = tpGuiItem("btn/energy_off", "menu.nova.side_config.energy")
    val TP_ENERGY_BTN_ON = tpGuiItem("btn/energy_on", "menu.nova.side_config.energy")
    val TP_ENERGY_BTN_SELECTED = tpGuiItem("btn/energy_selected", "menu.nova.side_config.energy")
    val TP_ITEM_BTN_OFF = tpGuiItem("btn/items_off", "menu.nova.side_config.items")
    val TP_ITEM_BTN_ON = tpGuiItem("btn/items_on", "menu.nova.side_config.items")
    val TP_ITEM_BTN_SELECTED = tpGuiItem("btn/items_selected", "menu.nova.side_config.items")
    val TP_FLUID_BTN_OFF = tpGuiItem("btn/fluids_off", "menu.nova.side_config.fluids")
    val TP_FLUID_BTN_ON = tpGuiItem("btn/fluids_on", "menu.nova.side_config.fluids")
    val TP_FLUID_BTN_SELECTED = tpGuiItem("btn/fluids_selected", "menu.nova.side_config.fluids")
    val TP_SIMPLE_MODE_BTN_OFF = tpGuiItem("btn/simple_mode_off", "menu.nova.side_config.simple_mode")
    val TP_SIMPLE_MODE_BTN_ON = tpGuiItem("btn/simple_mode_on", "menu.nova.side_config.simple_mode")
    val TP_ADVANCED_MODE_BTN_OFF = tpGuiItem("btn/advanced_mode_off", "menu.nova.side_config.advanced_mode")
    val TP_ADVANCED_MODE_BTN_ON = tpGuiItem("btn/advanced_mode_on", "menu.nova.side_config.advanced_mode")
    val TP_BLUE_BTN = tpGuiItem("btn/blue")
    val TP_GRAY_BTN = tpGuiItem("btn/gray")
    val TP_GREEN_BTN = tpGuiItem("btn/green")
    val TP_ORANGE_BTN = tpGuiItem("btn/orange")
    val TP_PINK_BTN = tpGuiItem("btn/pink")
    val TP_RED_BTN = tpGuiItem("btn/red")
    val TP_WHITE_BTN = tpGuiItem("btn/white")
    val TP_YELLOW_BTN = tpGuiItem("btn/yellow")
    
    // other
    val TP_ARROW_LEFT_OFF = tpGuiItem("arrow/left_off")
    val TP_ARROW_LEFT_ON = tpGuiItem("arrow/left_on")
    val TP_ARROW_RIGHT_OFF = tpGuiItem("arrow/right_off")
    val TP_ARROW_RIGHT_ON = tpGuiItem("arrow/right_on")
    val TP_ARROW_DOWN_OFF = tpGuiItem("arrow/down_off")
    val TP_ARROW_DOWN_ON = tpGuiItem("arrow/down_on")
    val TP_ARROW_UP_OFF = tpGuiItem("arrow/up_off")
    val TP_ARROW_UP_ON = tpGuiItem("arrow/up_on")
    val TP_SMALL_ARROW_LEFT_OFF = tpGuiItem("small_arrow/left_off")
    val TP_SMALL_ARROW_LEFT_ON = tpGuiItem("small_arrow/left_on")
    val TP_SMALL_ARROW_RIGHT_OFF = tpGuiItem("small_arrow/right_off")
    val TP_SMALL_ARROW_RIGHT_ON = tpGuiItem("small_arrow/right_on")
    val TP_SMALL_ARROW_DOWN_OFF = tpGuiItem("small_arrow/down_off")
    val TP_SMALL_ARROW_DOWN_ON = tpGuiItem("small_arrow/down_on")
    val TP_SMALL_ARROW_UP_OFF = tpGuiItem("small_arrow/up_off")
    val TP_SMALL_ARROW_UP_ON = tpGuiItem("small_arrow/up_on")
    val TP_BAR_BLUE = hiddenItem("gui/transparent/bar/blue") { selectModels(0..16, true) { createGuiModel("gui/bar/blue/$it", background = false, stretched = true) } }
    val TP_BAR_GREEN = hiddenItem("gui/transparent/bar/green") { selectModels(0..16, true) { createGuiModel("gui/bar/green/$it", background = false, stretched = true) } }
    val TP_BAR_RED = hiddenItem("gui/transparent/bar/red") { selectModels(0..16, true) { createGuiModel("gui/bar/red/$it", background = false, stretched = true) } }
    val TP_BAR_ORANGE = hiddenItem("gui/transparent/bar/orange") { selectModels(0..16, true) { createGuiModel("gui/bar/orange/$it", background = false, stretched = true) } }
    val TP_CHEATING_ON = tpGuiItem("cheating_on", "menu.nova.items.cheat_mode_item")
    val TP_CHEATING_OFF = tpGuiItem("cheating_off", "menu.nova.items.cheat_mode_item")
    val TP_COLOR_PICKER = tpGuiItem("color_picker")
    val TP_NUMBER = hiddenItem("number") { selectModels(0..999, true) { createGuiModel("gui/number/$it", stretched = false, background = false) } }
    val TP_SEARCH = tpGuiItem("search")
    val TP_STOPWATCH = tpGuiItem("stopwatch")
    //</editor-fold>
    
}

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object DefaultBlockOverlays {
    
    val BREAK_STAGE_OVERLAY = hiddenItem("break_stage_overlay") {
        selectModels(0..9, true) {
            getModelRawPath(ResourcePath("nova", "block/break_stage/$it"))
        }
    }
    
    val TRANSPARENT_BLOCK = hiddenItem("transparent_block") { 
        selectModel {
            getModelRawPath(ResourcePath("nova", "block/transparent"))
        }
    }
    
}

private fun item(name: String, run: NovaItemBuilder.() -> Unit): NovaItem {
    val builder = NovaItemBuilder(ResourceLocation("nova", name))
    builder.run()
    return builder.register()
}

private fun hiddenItem(
    name: String,
    localizedName: String = "",
    vararg itemBehaviors: ItemBehaviorHolder
): NovaItem = item(name) {
    localizedName(localizedName)
    behaviors(*itemBehaviors)
    hidden(true)
    models {
        itemType(PacketItems.SERVER_SIDE_MATERIAL)
    }
}

private fun hiddenItem(
    name: String,
    run: ItemModelLayoutBuilder.() -> Unit
): NovaItem = item(name) {
    hidden(true)
    models {
        itemType(PacketItems.SERVER_SIDE_MATERIAL)
        run()
    }
}

private fun guiItem(
    name: String,
    localizedName: String = "",
    stretched: Boolean = false
): NovaItem = item("gui/opaque/$name") {
    localizedName(localizedName)
    hidden(true)
    models { 
        itemType(PacketItems.SERVER_SIDE_MATERIAL)
        selectModel { createGuiModel("gui/$name", true, stretched) }
    }
}

private fun tpGuiItem(
    name: String,
    localizedName: String = "",
    stretched: Boolean = false
): NovaItem = item("gui/transparent/$name") {
    localizedName(localizedName)
    hidden(true)
    models { 
        itemType(PacketItems.SERVER_SIDE_MATERIAL)
        selectModel { createGuiModel("gui/$name", false, stretched) } 
    }
}
