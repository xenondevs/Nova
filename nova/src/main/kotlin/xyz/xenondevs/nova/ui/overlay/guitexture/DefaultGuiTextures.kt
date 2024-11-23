package xyz.xenondevs.nova.ui.overlay.guitexture

import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.layout.gui.GuiTextureAlignment
import xyz.xenondevs.nova.resources.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.util.data.Key
import xyz.xenondevs.nova.util.set

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object DefaultGuiTextures {
    
    val EMPTY_GUI = guiTexture("empty") {
        path("gui/empty")
    }
    val SEARCH = guiTexture("search") {
        alignment(GuiTextureAlignment.AnvilDefault)
        path("gui/search")
    }
    val ITEMS_0 = guiTexture("items_0") {
        path("gui/items/0")
    }
    val ITEMS_1 = guiTexture("items_1") {
        path("gui/items/1")
    }
    val ITEMS_2 = guiTexture("items_2") {
        path("gui/items/2")
    }
    val ITEMS_3 = guiTexture("items_3") {
        path("gui/items/3")
    }
    val ITEMS_4 = guiTexture("items_4") {
        path("gui/items/4")
    }
    val RECIPE_CRAFTING = guiTexture("recipe_crafting") {
        path("gui/recipe/crafting")
    }
    val RECIPE_SMELTING = guiTexture("recipe_smelting") {
        path("gui/recipe/furnace")
    }
    val RECIPE_SMITHING = guiTexture("recipe_smithing") {
        path("gui/recipe/smithing")
    }
    val RECIPE_CONVERSION = guiTexture("recipe_conversion") {
        path("gui/recipe/conversion")
    }
    val COLOR_PICKER = guiTexture("color_picker") {
        path("gui/color_picker")
    }
    
    private fun guiTexture(name: String, texture: GuiTextureLayoutBuilder.() -> Unit): GuiTexture {
        val id = Key(Nova, name)
        val texture = GuiTexture(id) { GuiTextureLayoutBuilder(id.namespace(), it).apply(texture).build() }
        NovaRegistries.GUI_TEXTURE[id] = texture
        return texture
    }
    
}