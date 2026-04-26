package xyz.xenondevs.nova.ui.overlay.guitexture

import net.kyori.adventure.text.Component
import org.joml.Vector2i
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistrar.guiTexture
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureAlignment
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture.TitlePosition.Alignment

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
object DefaultGuiTextures {
    
    // -- Vanilla --
    val ANVIL = guiTexture("anvil") {
        texture {
            path("gui/vanilla/anvil")
            alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.ANVIL_OFFSET))
        }
    }
    val CARTOGRAPHY_TABLE = guiTexture("cartography_table") {
        texture {
            path("gui/vanilla/cartography_table")
            alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.CARTOGRAPHY_TABLE_OFFSET))
        }
    }
    val CRAFTER = guiTexture("crafter") {
        texture {
            path("gui/vanilla/crafter")
            alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.CRAFTER_OFFSET))
        }
    }
    val DISPENSER = guiTexture("dispenser") {
        texture {
            path("gui/vanilla/dispenser")
            alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.DISPENSER_OFFSET))
        }
    }
    val GENERIC_9x1 = guiTexture("generic_9x1") { texture { path("gui/vanilla/generic_9x1") } }
    val GENERIC_9x2 = guiTexture("generic_9x2") { texture { path("gui/vanilla/generic_9x2") } }
    val GENERIC_9x3 = guiTexture("generic_9x3") { texture { path("gui/vanilla/generic_9x3") } }
    val GENERIC_9x4 = guiTexture("generic_9x4") { texture { path("gui/vanilla/generic_9x4") } }
    val GENERIC_9x5 = guiTexture("generic_9x5") { texture { path("gui/vanilla/generic_9x5") } }
    val GENERIC_9x6 = guiTexture("generic_9x6") { texture { path("gui/vanilla/generic_9x6") } }
    val GRINDSTONE = guiTexture("grindstone") {
        texture {
            path("gui/vanilla/grindstone")
            alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.GRINDSTONE_OFFSET))
        }
    }
    val HOPPER = guiTexture("hopper") {
        texture {
            path("gui/vanilla/hopper")
            alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.HOPPER_OFFSET))
        }
    }
    
    // -- Custom --
    val EMPTY_GUI = guiTexture("empty") { texture { path("gui/empty") } }
    val SEARCH = guiTexture("search") {
        texture {
            alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.ANVIL_OFFSET, 0, -19))
            path("gui/search")
        }
        inventoryLabel(false)
    }
    val SEARCH_RESULTS = guiTexture("search_results") { 
        texture { path("gui/search_results") }
        title { alignment(Alignment.LEFT, Vector2i(21, 18)) }
    }
    val ITEMS_0 = guiTexture("items_0") {
        texture { path("gui/items/0") }
        inventoryLabel(false)
    }
    val ITEMS_1 = guiTexture("items_1") {
        texture { path("gui/items/1") }
        inventoryLabel(false)
    }
    val ITEMS_2 = guiTexture("items_2") {
        texture { path("gui/items/2") }
        inventoryLabel(false)
    }
    val ITEMS_3 = guiTexture("items_3") {
        texture { path("gui/items/3") }
        inventoryLabel(false)
    }
    val ITEMS_4 = guiTexture("items_4") {
        texture { path("gui/items/4") }
        inventoryLabel(false)
    }
    val ITEMS_5 = guiTexture("items_5") {
        texture { path("gui/items/5") }
        inventoryLabel(false)
    }
    val ITEMS_6 = guiTexture("items_6") {
        texture { path("gui/items/6") }
        inventoryLabel(false)
    }
    val ITEMS_7 = guiTexture("items_7") {
        texture { path("gui/items/7") }
        inventoryLabel(false)
    }
    val ITEMS_8 = guiTexture("items_8") {
        texture { path("gui/items/8") }
        inventoryLabel(false)
    }
    val ITEMS_9 = guiTexture("items_9") {
        texture { path("gui/items/9") }
        inventoryLabel(false)
    }
    val RECIPE_CRAFTING = guiTexture("recipe_crafting") { texture { path("gui/recipe/crafting") } }
    val RECIPE_SMITHING = guiTexture("recipe_smithing") { texture { path("gui/recipe/smithing") } }
    val RECIPE_CONVERSION = guiTexture("recipe_conversion") { texture { path("gui/recipe/conversion") } }
    val SIDE_CONFIG = guiTexture("side_config") {
        title { line(Component.translatable("menu.nova.side_config"), Alignment.LEFT, Vector2i(21, 18)) }
        texture { path("gui/side_config") }
    }
    
    
}