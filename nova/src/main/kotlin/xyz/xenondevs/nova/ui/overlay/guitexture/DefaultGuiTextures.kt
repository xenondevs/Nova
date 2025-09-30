package xyz.xenondevs.nova.ui.overlay.guitexture

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureAlignment
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.util.set

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object DefaultGuiTextures {
    
    // -- Vanilla --
    val ANVIL = guiTexture("anvil") {
        path("gui/vanilla/anvil")
        alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.ANVIL_OFFSET))
    }
    val CARTOGRAPHY_TABLE = guiTexture("cartography_table") {
        path("gui/vanilla/cartography_table")
        alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.CARTOGRAPHY_TABLE_OFFSET))
    }
    val CRAFTER = guiTexture("crafter") {
        path("gui/vanilla/crafter")
        alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.CRAFTER_OFFSET))
    }
    val DISPENSER = guiTexture("dispenser") {
        path("gui/vanilla/dispenser")
        alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.DISPENSER_OFFSET))
    }
    val GENERIC_9x1 = guiTexture("generic_9x1") { path("gui/vanilla/generic_9x1") }
    val GENERIC_9x2 = guiTexture("generic_9x2") { path("gui/vanilla/generic_9x2") }
    val GENERIC_9x3 = guiTexture("generic_9x3") { path("gui/vanilla/generic_9x3") }
    val GENERIC_9x4 = guiTexture("generic_9x4") { path("gui/vanilla/generic_9x4") }
    val GENERIC_9x5 = guiTexture("generic_9x5") { path("gui/vanilla/generic_9x5") }
    val GENERIC_9x6 = guiTexture("generic_9x6") { path("gui/vanilla/generic_9x6") }
    val GRINDSTONE = guiTexture("grindstone") {
        path("gui/vanilla/grindstone")
        alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.GRINDSTONE_OFFSET))
    }
    val HOPPER = guiTexture("hopper") {
        path("gui/vanilla/hopper")
        alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.HOPPER_OFFSET))
    }
    
    // -- Custom --
    val EMPTY_GUI = guiTexture("empty") { path("gui/empty") }
    val SEARCH = guiTexture("search") {
        alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.ANVIL_OFFSET, 0, -19))
        path("gui/search")
        inventoryLabel(false)
    }
    val SEARCH_RESULTS = guiTexture("search_results") { path("gui/search_results") }
    val ITEMS_0 = guiTexture("items_0") {
        path("gui/items/0")
        inventoryLabel(false)
    }
    val ITEMS_1 = guiTexture("items_1") {
        path("gui/items/1")
        inventoryLabel(false)
    }
    val ITEMS_2 = guiTexture("items_2") {
        path("gui/items/2")
        inventoryLabel(false)
    }
    val ITEMS_3 = guiTexture("items_3") {
        path("gui/items/3")
        inventoryLabel(false)
    }
    val ITEMS_4 = guiTexture("items_4") {
        path("gui/items/4")
        inventoryLabel(false)
    }
    val ITEMS_5 = guiTexture("items_5") {
        path("gui/items/5")
        inventoryLabel(false)
    }
    val ITEMS_6 = guiTexture("items_6") {
        path("gui/items/6")
        inventoryLabel(false)
    }
    val ITEMS_7 = guiTexture("items_7") {
        path("gui/items/7")
        inventoryLabel(false)
    }
    val ITEMS_8 = guiTexture("items_8") {
        path("gui/items/8")
        inventoryLabel(false)
    }
    val ITEMS_9 = guiTexture("items_9") {
        path("gui/items/9")
        inventoryLabel(false)
    }
    val RECIPE_CRAFTING = guiTexture("recipe_crafting") { path("gui/recipe/crafting") }
    val RECIPE_SMITHING = guiTexture("recipe_smithing") { path("gui/recipe/smithing") }
    val RECIPE_CONVERSION = guiTexture("recipe_conversion") { path("gui/recipe/conversion") }
    
    private fun guiTexture(name: String, texture: GuiTextureLayoutBuilder.() -> Unit): GuiTexture {
        val id = Key.key("nova", name)
        val texture = GuiTexture(id) { GuiTextureLayoutBuilder(id.namespace(), it).apply(texture).build() }
        NovaRegistries.GUI_TEXTURE[id] = texture
        return texture
    }
    
}