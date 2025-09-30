package xyz.xenondevs.nova.ui.menu

import net.kyori.adventure.text.Component
import net.minecraft.world.inventory.MenuType
import org.joml.Vector2i
import org.joml.Vector2ic
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.clientbound.ClientboundOpenScreenPacketEvent
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureAlignment
import xyz.xenondevs.nova.resources.builder.task.FontChar
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.ui.overlay.MovedFonts
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.util.component.adventure.charsIterator
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.moveToStart
import xyz.xenondevs.nova.util.component.adventure.toAdventureComponent
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent

/**
 * Adds the vanilla gui textures back to all screens that don't use a custom gui texture.
 */
@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object VanillaGuiTextureInjector : PacketListener {
    
    private val _requiredVerticalOffsets = HashSet<Int>()
    val requiredVerticalOffsets: Set<Int>
        get() = _requiredVerticalOffsets
    
    // offsets from title start to inventory label start
    private val ANVIL_OFFSET: Vector2ic = offset(GuiTextureAlignment.ANVIL_OFFSET, 8, 79)
    private val BLAST_FURNACE_OFFSET: Vector2ic = offset(GuiTextureAlignment.BLAST_FURNACE_OFFSET, 8, 79)
    private val BREWING_STAND_OFFSET: Vector2ic = offset(GuiTextureAlignment.BREWING_STAND_OFFSET, 8, 79)
    private val CARTOGRAPHY_TABLE_OFFSET: Vector2ic = offset(GuiTextureAlignment.CARTOGRAPHY_TABLE_OFFSET, 8, 79)
    private val CRAFTER_3x3_OFFSET: Vector2ic = offset(GuiTextureAlignment.CRAFTER_OFFSET, 8, 79)
    private val CRAFTING_OFFSET: Vector2ic = offset(GuiTextureAlignment.CRAFTING_TABLE_OFFSET, 8, 79)
    private val ENCHANTMENT_OFFSET: Vector2ic = offset(GuiTextureAlignment.ENCHANTMENT_TABLE_OFFSET, 8, 79)
    private val FURNACE_OFFSET: Vector2ic = offset(GuiTextureAlignment.FURNACE_OFFSET, 8, 79)
    private val GENERIC_9x1_OFFSET: Vector2ic = offset(GuiTextureAlignment.CHEST_OFFSET, 8, 45)
    private val GENERIC_9x2_OFFSET: Vector2ic = offset(GuiTextureAlignment.CHEST_OFFSET, 8, 63)
    private val GENERIC_9x3_OFFSET: Vector2ic = offset(GuiTextureAlignment.CHEST_OFFSET, 8, 81)
    private val GENERIC_9x4_OFFSET: Vector2ic = offset(GuiTextureAlignment.CHEST_OFFSET, 8, 99)
    private val GENERIC_9x5_OFFSET: Vector2ic = offset(GuiTextureAlignment.CHEST_OFFSET, 8, 117)
    private val GENERIC_9x6_OFFSET: Vector2ic = offset(GuiTextureAlignment.CHEST_OFFSET, 8, 135)
    private val GENERIC_3x3_OFFSET: Vector2ic = offset(GuiTextureAlignment.DISPENSER_OFFSET, 8, 79)
    private val GRINDSTONE_OFFSET: Vector2ic = offset(GuiTextureAlignment.GRINDSTONE_OFFSET, 8, 79)
    private val HOPPER_OFFSET: Vector2ic = offset(GuiTextureAlignment.HOPPER_OFFSET, 8, 46)
    private val LOOM_OFFSET: Vector2ic = offset(GuiTextureAlignment.LOOM_OFFSET, 8, 79)
    private val SHULKER_BOX_OFFSET: Vector2ic = offset(GuiTextureAlignment.SHULKER_BOX_OFFSET, 8, 81)
    private val SMITHING_OFFSET: Vector2ic = offset(GuiTextureAlignment.SMITHING_TABLE_OFFSET, 8, 79)
    private val SMOKER_OFFSET: Vector2ic = offset(GuiTextureAlignment.SMOKER_OFFSET, 8, 79)
    private val STONECUTTER_OFFSET: Vector2ic = offset(GuiTextureAlignment.STONECUTTER_OFFSET, 8, 79)
    
    private fun offset(base: Vector2ic, x: Int, y: Int): Vector2ic {
        val new = Vector2i(base).add(x, y)
        _requiredVerticalOffsets += new.y
        return new
    }
    
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
        val vanillaGuiTexture = menuType.guiTexture
        val guiTexture = findGuiTexture(oldTitle)
        if (guiTexture == null && vanillaGuiTexture != null) {
            newTitle
                .append(vanillaGuiTexture.component)
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
        if (offset != null && (guiTexture == null || ResourceLookups.GUI_TEXTURE[guiTexture]?.hasInventoryLabel != false)) {
            newTitle
                .move(offset.x())
                .append(MovedFonts.moveVertically(Component.translatable("container.nova.inventory"), offset.y()))
                .moveToStart(lang)
        }
        
        event.title = newTitle.build().toNMSComponent()
    }
    
    private fun findGuiTexture(title: Component): GuiTexture? {
        var guiTexture: FontChar? = null
        for (char in title.charsIterator()) {
            val font = char.style.font()
            if (font?.toString()?.startsWith("nova:gui_") == true) {
                guiTexture = FontChar(font, char.char.code)
                break
            }
        }
        
        return ResourceLookups.GUI_TEXTURE_BY_FONT_CHAR[guiTexture]
    }
    
    private val MenuType<*>.guiTexture: GuiTexture?
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
            MenuType.ANVIL -> ANVIL_OFFSET
            MenuType.BLAST_FURNACE -> BLAST_FURNACE_OFFSET
            MenuType.BREWING_STAND -> BREWING_STAND_OFFSET
            MenuType.CARTOGRAPHY_TABLE -> CARTOGRAPHY_TABLE_OFFSET
            MenuType.CRAFTER_3x3 -> CRAFTER_3x3_OFFSET
            MenuType.CRAFTING -> CRAFTING_OFFSET
            MenuType.ENCHANTMENT -> ENCHANTMENT_OFFSET
            MenuType.FURNACE -> FURNACE_OFFSET
            MenuType.GENERIC_9x1 -> GENERIC_9x1_OFFSET
            MenuType.GENERIC_9x2 -> GENERIC_9x2_OFFSET
            MenuType.GENERIC_9x3 -> GENERIC_9x3_OFFSET
            MenuType.GENERIC_9x4 -> GENERIC_9x4_OFFSET
            MenuType.GENERIC_9x5 -> GENERIC_9x5_OFFSET
            MenuType.GENERIC_9x6 -> GENERIC_9x6_OFFSET
            MenuType.GENERIC_3x3 -> GENERIC_3x3_OFFSET
            MenuType.GRINDSTONE -> GRINDSTONE_OFFSET
            MenuType.HOPPER -> HOPPER_OFFSET
            MenuType.LOOM -> LOOM_OFFSET
            MenuType.SHULKER_BOX -> SHULKER_BOX_OFFSET
            MenuType.SMITHING -> SMITHING_OFFSET
            MenuType.SMOKER -> SMOKER_OFFSET
            MenuType.STONECUTTER -> STONECUTTER_OFFSET
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