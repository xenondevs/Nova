package xyz.xenondevs.nova.ui.menu

import net.kyori.adventure.text.Component
import net.minecraft.world.inventory.MenuType
import org.joml.Vector2ic
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.clientbound.ClientboundOpenScreenPacketEvent
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureAlignment
import xyz.xenondevs.nova.resources.builder.task.FontChar
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.ui.overlay.MovedFonts
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.util.component.adventure.StyledElement
import xyz.xenondevs.nova.util.component.adventure.elements
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.moveToStart
import xyz.xenondevs.nova.util.component.adventure.toAdventureComponent
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent

/**
 * Adds the vanilla gui textures back to all screens that don't use a custom gui texture.
 */
@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object VanillaGuiTextureInjector : PacketListener {
    
    private val _requiredVerticalOffsets = hashSetOf(
        GuiTextureAlignment.ANVIL_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.BLAST_FURNACE_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.BREWING_STAND_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.CARTOGRAPHY_TABLE_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.CRAFTER_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.CRAFTING_TABLE_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.ENCHANTMENT_TABLE_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.FURNACE_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.GENERIC_9x1_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.GENERIC_9x2_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.GENERIC_9x3_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.GENERIC_9x4_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.GENERIC_9x5_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.GENERIC_9x6_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.GENERIC_3x3_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.GRINDSTONE_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.HOPPER_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.LOOM_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.SHULKER_BOX_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.SMITHING_TABLE_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.SMOKER_INVENTORY_LABEL_OFFSET_FROM_TITLE.y(),
        GuiTextureAlignment.STONECUTTER_INVENTORY_LABEL_OFFSET_FROM_TITLE.y()
    )
    val requiredVerticalOffsets: Set<Int>
        get() = _requiredVerticalOffsets

    @InitFun
    private fun init() {
        registerPacketListener()
    }
    
    @Suppress("DEPRECATION")
    @PacketHandler
    private fun handleScreenOpen(event: ClientboundOpenScreenPacketEvent) {
        /*
         * Merchant:
         * The centered title is influenced by the villager level (e.g. Novice, etc.).
         * This level is sent in a different packet, and can also be updated while the inventory is open.
         * This would require Nova to update the title via a new open screen packet that includes the villager level in the title.
         * This would in turn require Nova to track the menu state, due to potentially packet-based menus.
         * I don't think this is worth the effort just for the inventory label.
         */
        val menuType = event.type
        if (menuType == MenuType.MERCHANT)
            return
        
        // translate this server-side to prevent different translations on the client from causing incorrect offsets
        val oldTitle = LocaleManager.render(event.title.toAdventureComponent(), event.player.locale())
        val lang = event.player.locale
        
        val newTitle = Component.text()
        
        // add vanilla gui texture if there is no custom gui texture
        val vanillaGuiTexture = menuType.guiTexture?.get()?.component?.get()
        val guiTexture = findGuiTexture(oldTitle)
        if (guiTexture == null && vanillaGuiTexture != null) {
            newTitle
                .append(vanillaGuiTexture)
                .moveToStart(lang)
        }
        
        // add old title, centered if required
        if (menuType.hasCenteredTitle) {
            val halfWidth = CharSizes.calculateComponentWidth(oldTitle, lang) / 2.0
            newTitle
                .move(-halfWidth)
                .append(oldTitle)
                .move(-halfWidth)
        } else {
            newTitle
                .append(oldTitle)
                .moveToStart(lang)
        }
        
        // add inventory label if required
        // (menu type has inventory label and there is no custom gui texture that doesn't want it)
        val offset = menuType.offsetToInventoryLabel
        if (offset != null && (guiTexture == null || guiTexture.hasInventoryLabel)) {
            newTitle
                .move(offset.x())
                .append(MovedFonts.moveVertically(Component.translatable("container.nova.inventory"), offset.y()))
                .moveToStart(lang)
        }
        
        event.title = newTitle.build().toNMSComponent()
    }
    
    private fun findGuiTexture(title: Component): GuiTexture? {
        var guiTexture: FontChar? = null
        for (el in title.elements()) {
            val font = el.style.font()
            if (el is StyledElement.CodePoint && font?.toString()?.startsWith("nova:gui_") == true) {
                guiTexture = FontChar(font, el.codePoint)
                break
            }
        }
        
        return ResourceLookups.guiTextureByFontChar[guiTexture]?.get()
    }
    
    private val MenuType<*>.guiTexture: RegistryEntry.Nova<GuiTexture>?
        get() = when (this) {
            MenuType.ANVIL -> DefaultGuiTextures.ANVIL
            MenuType.CARTOGRAPHY_TABLE -> DefaultGuiTextures.CARTOGRAPHY_TABLE
            MenuType.CRAFTER_3x3 -> DefaultGuiTextures.CRAFTER
            MenuType.GENERIC_9x1 -> DefaultGuiTextures.GENERIC_9x1
            MenuType.GENERIC_9x2 -> DefaultGuiTextures.GENERIC_9x2
            MenuType.GENERIC_9x3 -> DefaultGuiTextures.GENERIC_9x3
            MenuType.GENERIC_9x4 -> DefaultGuiTextures.GENERIC_9x4
            MenuType.GENERIC_9x5 -> DefaultGuiTextures.GENERIC_9x5
            MenuType.GENERIC_9x6 -> DefaultGuiTextures.GENERIC_9x6
            MenuType.GENERIC_3x3 -> DefaultGuiTextures.DISPENSER
            MenuType.GRINDSTONE -> DefaultGuiTextures.GRINDSTONE
            MenuType.HOPPER -> DefaultGuiTextures.HOPPER
            else -> null
        }
    
    private val MenuType<*>.offsetToInventoryLabel: Vector2ic?
        get() = when (this) {
            MenuType.ANVIL -> GuiTextureAlignment.ANVIL_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.BLAST_FURNACE -> GuiTextureAlignment.BLAST_FURNACE_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.BREWING_STAND -> GuiTextureAlignment.BREWING_STAND_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.CARTOGRAPHY_TABLE -> GuiTextureAlignment.CARTOGRAPHY_TABLE_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.CRAFTER_3x3 -> GuiTextureAlignment.CRAFTER_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.CRAFTING -> GuiTextureAlignment.CRAFTING_TABLE_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.ENCHANTMENT -> GuiTextureAlignment.ENCHANTMENT_TABLE_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.FURNACE -> GuiTextureAlignment.FURNACE_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.GENERIC_9x1 -> GuiTextureAlignment.GENERIC_9x1_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.GENERIC_9x2 -> GuiTextureAlignment.GENERIC_9x2_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.GENERIC_9x3 -> GuiTextureAlignment.GENERIC_9x3_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.GENERIC_9x4 -> GuiTextureAlignment.GENERIC_9x4_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.GENERIC_9x5 -> GuiTextureAlignment.GENERIC_9x5_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.GENERIC_9x6 -> GuiTextureAlignment.GENERIC_9x6_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.GENERIC_3x3 -> GuiTextureAlignment.GENERIC_3x3_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.GRINDSTONE -> GuiTextureAlignment.GRINDSTONE_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.HOPPER -> GuiTextureAlignment.HOPPER_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.LOOM -> GuiTextureAlignment.LOOM_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.SHULKER_BOX -> GuiTextureAlignment.SHULKER_BOX_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.SMITHING -> GuiTextureAlignment.SMITHING_TABLE_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.SMOKER -> GuiTextureAlignment.SMOKER_INVENTORY_LABEL_OFFSET_FROM_TITLE
            MenuType.STONECUTTER -> GuiTextureAlignment.STONECUTTER_INVENTORY_LABEL_OFFSET_FROM_TITLE
            else -> null
        }
    
    private val MenuType<*>.hasCenteredTitle: Boolean
        get() = when (this) {
            MenuType.BREWING_STAND,
            MenuType.CRAFTER_3x3,
            MenuType.GENERIC_3x3,
            MenuType.FURNACE,
            MenuType.BLAST_FURNACE,
            MenuType.SMOKER -> true
            
            else -> false
        }
    
}