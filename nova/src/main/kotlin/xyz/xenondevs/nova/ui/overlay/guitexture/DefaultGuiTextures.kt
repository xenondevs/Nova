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
    val ANVIL = guiTexture("vanilla/anvil") {
        texture { alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.ANVIL_OFFSET)) }
    }
    val CARTOGRAPHY_TABLE = guiTexture("vanilla/cartography_table") {
        texture { alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.CARTOGRAPHY_TABLE_OFFSET)) }
    }
    val CRAFTER = guiTexture("vanilla/crafter") {
        texture { alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.CRAFTER_OFFSET)) }
    }
    val DISPENSER = guiTexture("vanilla/dispenser") {
        texture { alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.DISPENSER_OFFSET)) }
    }
    val GENERIC_9x1 = guiTexture("vanilla/generic_9x1") {}
    val GENERIC_9x2 = guiTexture("vanilla/generic_9x2") {}
    val GENERIC_9x3 = guiTexture("vanilla/generic_9x3") {}
    val GENERIC_9x4 = guiTexture("vanilla/generic_9x4") {}
    val GENERIC_9x5 = guiTexture("vanilla/generic_9x5") {}
    val GENERIC_9x6 = guiTexture("vanilla/generic_9x6") {}
    val GRINDSTONE = guiTexture("vanilla/grindstone") {
        texture { alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.GRINDSTONE_OFFSET)) }
    }
    val HOPPER = guiTexture("vanilla/hopper") {
        texture { alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.HOPPER_OFFSET)) }
    }
    
    // -- Custom --
    val EMPTY_GUI = guiTexture("empty") {}
    val SEARCH = guiTexture("search") {
        texture { alignment(GuiTextureAlignment.TopLeft(GuiTextureAlignment.ANVIL_OFFSET, 0, -19)) }
        inventoryLabel(false)
    }
    val SEARCH_RESULTS = guiTexture("search_results") {
        title { dynamicLine(Alignment.LEFT, Vector2i(21, 18)) }
    }
    val ITEMS_0 = guiTexture("items/0") {
        inventoryLabel(false)
    }
    val ITEMS_1 = guiTexture("items/1") {
        inventoryLabel(false)
    }
    val ITEMS_2 = guiTexture("items/2") {
        inventoryLabel(false)
    }
    val ITEMS_3 = guiTexture("items/3") {
        inventoryLabel(false)
    }
    val ITEMS_4 = guiTexture("items/4") {
        inventoryLabel(false)
    }
    val ITEMS_5 = guiTexture("items/5") {
        inventoryLabel(false)
    }
    val ITEMS_6 = guiTexture("items/6") {
        inventoryLabel(false)
    }
    val ITEMS_7 = guiTexture("items/7") {
        inventoryLabel(false)
    }
    val ITEMS_8 = guiTexture("items/8") {
        inventoryLabel(false)
    }
    val ITEMS_9 = guiTexture("items/9") {
        inventoryLabel(false)
    }
    val RECIPE_CRAFTING = guiTexture("recipe/crafting") {
        title { dynamicLine(Alignment.CENTER, Vector2i(0, 34)) }
    }
    val RECIPE_SMITHING = guiTexture("recipe/smithing") {
        title { dynamicLine(Alignment.CENTER, Vector2i(0, 34)) }
    }
    val RECIPE_CONVERSION = guiTexture("recipe/conversion") {
        title { dynamicLine(Alignment.CENTER, Vector2i(0, 34)) }
    }
    val SIDE_CONFIG = guiTexture("side_config") {
        title { staticLine(Component.translatable("menu.nova.side_config"), Alignment.LEFT, Vector2i(21, 18)) }
    }
    val TAGS = guiTexture("tags") {}
    
}
