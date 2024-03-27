package xyz.xenondevs.nova.ui.overlay.guitexture

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object DefaultGuiTextures {
    
    val EMPTY_GUI = guiTexture("empty") { texture { path("empty") } }
    val SEARCH = guiTexture("search") { texture { path("search") } }
    val ITEMS_0 = guiTexture("items_0") { texture { path("items/0") } }
    val ITEMS_1 = guiTexture("items_1") { texture { path("items/1") } }
    val ITEMS_2 = guiTexture("items_2") { texture { path("items/2") } }
    val ITEMS_3 = guiTexture("items_3") { texture { path("items/3") } }
    val ITEMS_4 = guiTexture("items_4") { texture { path("items/4") } }
    val RECIPE_CRAFTING = guiTexture("recipe_crafting") { texture { path("recipe_crafting") } }
    val RECIPE_SMELTING = guiTexture("recipe_smelting") { texture { path("recipe_smelting") } }
    val RECIPE_SMITHING = guiTexture("recipe_smithing") { texture { path("recipe_smithing") } }
    val RECIPE_CONVERSION = guiTexture("recipe_conversion") { texture { path("recipe_conversion") } }
    val COLOR_PICKER = guiTexture("color_picker") { texture { path("color_picker") } }
    
    private fun guiTexture(name: String, texture: GuiTextureBuilder.() -> Unit): GuiTexture =
        GuiTextureBuilder(ResourceLocation("nova", name)).apply(texture).register()
    
}