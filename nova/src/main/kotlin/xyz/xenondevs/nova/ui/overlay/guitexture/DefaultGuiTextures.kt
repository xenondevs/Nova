package xyz.xenondevs.nova.ui.overlay.guitexture

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.layout.gui.GuiTextureAlignment

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object DefaultGuiTextures {
    
    val EMPTY_GUI = guiTexture("empty") {
        texture {
            path("gui/empty")
        }
    }
    val SEARCH = guiTexture("search") {
        texture {
            alignment(GuiTextureAlignment.AnvilDefault)
            path("gui/search")
        }
    }
    val ITEMS_0 = guiTexture("items_0") {
        texture {
            path("gui/items/0")
        }
    }
    val ITEMS_1 = guiTexture("items_1") {
        texture {
            path("gui/items/1")
        }
    }
    val ITEMS_2 = guiTexture("items_2") {
        texture {
            path("gui/items/2")
        }
    }
    val ITEMS_3 = guiTexture("items_3") {
        texture {
            path("gui/items/3")
        }
    }
    val ITEMS_4 = guiTexture("items_4") {
        texture {
            path("gui/items/4")
        }
    }
    val RECIPE_CRAFTING = guiTexture("recipe_crafting") {
        texture {
            path("gui/recipe/crafting")
        }
    }
    val RECIPE_SMELTING = guiTexture("recipe_smelting") {
        texture {
            path("gui/recipe/furnace")
        }
    }
    val RECIPE_SMITHING = guiTexture("recipe_smithing") {
        texture {
            path("gui/recipe/smithing")
        }
    }
    val RECIPE_CONVERSION = guiTexture("recipe_conversion") {
        texture {
            path("gui/recipe/conversion")
        }
    }
    val COLOR_PICKER = guiTexture("color_picker") {
        texture {
            path("gui/color_picker")
        }
    }
    
    private fun guiTexture(name: String, texture: GuiTextureBuilder.() -> Unit): GuiTexture =
        GuiTextureBuilder(ResourceLocation.fromNamespaceAndPath("nova", name)).apply(texture).register()
    
}