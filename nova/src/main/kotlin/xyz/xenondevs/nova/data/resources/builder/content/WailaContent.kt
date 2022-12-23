package xyz.xenondevs.nova.data.resources.builder.content

import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.util.enumMapOf
import xyz.xenondevs.renderer.MinecraftModelRenderer
import java.io.File
import java.util.logging.Level

//<editor-fold desc="Hardcoded Textures", defaultstate="collapsed">
private val MATERIAL_TEXTURES = enumMapOf(
    // doors
    Material.IRON_DOOR to "item/iron_door",
    Material.OAK_DOOR to "item/oak_door",
    Material.SPRUCE_DOOR to "item/spruce_door",
    Material.BIRCH_DOOR to "item/birch_door",
    Material.JUNGLE_DOOR to "item/jungle_door",
    Material.ACACIA_DOOR to "item/acacia_door",
    Material.DARK_OAK_DOOR to "item/dark_oak_door",
    Material.MANGROVE_DOOR to "item/mangrove_door",
    Material.CRIMSON_DOOR to "item/crimson_door",
    Material.WARPED_DOOR to "item/warped_door",
    
    // signs
    Material.OAK_SIGN to "item/oak_sign",
    Material.SPRUCE_SIGN to "item/spruce_sign",
    Material.BIRCH_SIGN to "item/birch_sign",
    Material.JUNGLE_SIGN to "item/jungle_sign",
    Material.ACACIA_SIGN to "item/acacia_sign",
    Material.DARK_OAK_SIGN to "item/dark_oak_sign",
    Material.MANGROVE_SIGN to "item/mangrove_sign",
    Material.CRIMSON_SIGN to "item/crimson_sign",
    Material.WARPED_SIGN to "item/warped_sign",
    Material.BAMBOO_SIGN to "item/bamboo_sign",
    
    // hanging signs
    Material.OAK_HANGING_SIGN to "item/oak_hanging_sign",
    Material.SPRUCE_HANGING_SIGN to "item/spruce_hanging_sign",
    Material.BIRCH_HANGING_SIGN to "item/birch_hanging_sign",
    Material.JUNGLE_HANGING_SIGN to "item/jungle_hanging_sign",
    Material.ACACIA_HANGING_SIGN to "item/acacia_hanging_sign",
    Material.DARK_OAK_HANGING_SIGN to "item/dark_oak_hanging_sign",
    Material.MANGROVE_HANGING_SIGN to "item/mangrove_hanging_sign",
    Material.CRIMSON_HANGING_SIGN to "item/crimson_hanging_sign",
    Material.WARPED_HANGING_SIGN to "item/warped_hanging_sign",
    Material.BAMBOO_HANGING_SIGN to "item/bamboo_hanging_sign",
    
    // foliage
    Material.OAK_SAPLING to null,
    Material.SPRUCE_SAPLING to null,
    Material.BIRCH_SAPLING to null,
    Material.JUNGLE_SAPLING to null,
    Material.ACACIA_SAPLING to null,
    Material.DARK_OAK_SAPLING to null,
    Material.MANGROVE_PROPAGULE to null,
    Material.BAMBOO_SAPLING to "block/bamboo_stage0",
    Material.DEAD_BUSH to null,
    Material.SEAGRASS to null,
    Material.SEA_PICKLE to null,
    Material.CRIMSON_ROOTS to null,
    Material.WARPED_ROOTS to null,
    Material.NETHER_SPROUTS to null,
    Material.WEEPING_VINES to null,
    Material.TWISTING_VINES to null,
    Material.HANGING_ROOTS to null,
    Material.GLOW_LICHEN to null,
    Material.SCULK_VEIN to null,
    Material.SUGAR_CANE to null,
    
    // flowers
    Material.DANDELION to null,
    Material.POPPY to null,
    Material.BLUE_ORCHID to null,
    Material.ALLIUM to null,
    Material.AZURE_BLUET to null,
    Material.RED_TULIP to null,
    Material.ORANGE_TULIP to null,
    Material.WHITE_TULIP to null,
    Material.PINK_TULIP to null,
    Material.OXEYE_DAISY to null,
    Material.LILY_OF_THE_VALLEY to null,
    Material.WITHER_ROSE to null,
    Material.CORNFLOWER to null,
    Material.SUNFLOWER to "block/sunflower_front",
    Material.LILAC to "block/lilac_top",
    Material.ROSE_BUSH to "block/rose_bush_top",
    Material.PEONY to "block/peony_top",
    
    // mushrooms
    Material.BROWN_MUSHROOM to null,
    Material.RED_MUSHROOM to null,
    Material.CRIMSON_FUNGUS to null,
    Material.WARPED_FUNGUS to null,
    
    // coral
    Material.TUBE_CORAL to null,
    Material.BRAIN_CORAL to null,
    Material.BUBBLE_CORAL to null,
    Material.FIRE_CORAL to null,
    Material.HORN_CORAL to null,
    Material.TUBE_CORAL_FAN to null,
    Material.BRAIN_CORAL_FAN to null,
    Material.BUBBLE_CORAL_FAN to null,
    Material.FIRE_CORAL_FAN to null,
    Material.HORN_CORAL_FAN to null,
    Material.DEAD_BUBBLE_CORAL to null,
    Material.DEAD_BRAIN_CORAL to null,
    Material.DEAD_FIRE_CORAL to null,
    Material.DEAD_HORN_CORAL to null,
    Material.DEAD_TUBE_CORAL to null,
    Material.DEAD_TUBE_CORAL_FAN to null,
    Material.DEAD_BRAIN_CORAL_FAN to null,
    Material.DEAD_BUBBLE_CORAL_FAN to null,
    Material.DEAD_FIRE_CORAL_FAN to null,
    Material.DEAD_HORN_CORAL_FAN to null,
    
    // glass pane
    Material.GLASS_PANE to "block/glass",
    Material.WHITE_STAINED_GLASS_PANE to "block/white_stained_glass",
    Material.ORANGE_STAINED_GLASS_PANE to "block/orange_stained_glass",
    Material.MAGENTA_STAINED_GLASS_PANE to "block/magenta_stained_glass",
    Material.LIGHT_BLUE_STAINED_GLASS_PANE to "block/light_blue_stained_glass",
    Material.YELLOW_STAINED_GLASS_PANE to "block/yellow_stained_glass",
    Material.LIME_STAINED_GLASS_PANE to "block/lime_stained_glass",
    Material.PINK_STAINED_GLASS_PANE to "block/pink_stained_glass",
    Material.GRAY_STAINED_GLASS_PANE to "block/gray_stained_glass",
    Material.LIGHT_GRAY_STAINED_GLASS_PANE to "block/light_gray_stained_glass",
    Material.CYAN_STAINED_GLASS_PANE to "block/cyan_stained_glass",
    Material.PURPLE_STAINED_GLASS_PANE to "block/purple_stained_glass",
    Material.BLUE_STAINED_GLASS_PANE to "block/blue_stained_glass",
    Material.BROWN_STAINED_GLASS_PANE to "block/brown_stained_glass",
    Material.GREEN_STAINED_GLASS_PANE to "block/green_stained_glass",
    Material.RED_STAINED_GLASS_PANE to "block/red_stained_glass",
    Material.BLACK_STAINED_GLASS_PANE to "block/black_stained_glass",
    
    // amethyst
    Material.SMALL_AMETHYST_BUD to null,
    Material.MEDIUM_AMETHYST_BUD to null,
    Material.LARGE_AMETHYST_BUD to null,
    Material.AMETHYST_CLUSTER to null,
    
    // misc
    Material.TORCH to null,
    Material.REDSTONE_TORCH to null,
    Material.SOUL_TORCH to null,
    Material.LADDER to null,
    Material.CHAIN to "item/chain",
    Material.IRON_BARS to null,
    Material.LEVER to null,
    Material.TRIPWIRE_HOOK to null,
    Material.FROGSPAWN to null,
    Material.STRING to "item/string",
    Material.COBWEB to null,
    Material.BARRIER to "item/barrier",
    Material.STRUCTURE_VOID to "item/structure_void",
    Material.POINTED_DRIPSTONE to "item/pointed_dripstone",
    Material.RAIL to null,
    Material.REDSTONE_WIRE to "item/redstone",
    Material.BELL to "item/bell"
)

private val TEXTURES = setOf(
    "beetroots_stage0", "beetroots_stage1", "beetroots_stage2", "beetroots_stage3",
    "carrots_stage0", "carrots_stage1", "carrots_stage2", "carrots_stage3",
    "nether_wart_stage0", "nether_wart_stage1", "nether_wart_stage2",
    "potatoes_stage0", "potatoes_stage1", "potatoes_stage2", "potatoes_stage3",
    "sweet_berry_bush_stage0", "sweet_berry_bush_stage1", "sweet_berry_bush_stage2", "sweet_berry_bush_stage3",
    "wheat_stage0", "wheat_stage1", "wheat_stage2", "wheat_stage3", "wheat_stage4", "wheat_stage5", "wheat_stage6", "wheat_stage7",
    "activator_rail", "activator_rail_on", "detector_rail", "detector_rail_on", "powered_rail", "powered_rail_on"
)
//</editor-fold>

private const val SIZE = 32
private const val ASCENT = -4

private val WAILA_ENABLED by configReloadable { DEFAULT_CONFIG.getBoolean("waila.enabled") }

internal class WailaContent : FontContent<FontChar, WailaContent.WailaIconData>(Resources::updateWailaDataLookup) {
    
    init {
        if (WAILA_ENABLED) {
            writeHardcodedTextures()
            renderCustomItemServiceBlocks()
        }
    }
    
    private fun renderCustomItemServiceBlocks() {
        try {
            val renderer = MinecraftModelRenderer(
                512, 512,
                128, 128,
                listOf(ResourcePackBuilder.MCASSETS_DIR, ResourcePackBuilder.PACK_DIR),
                true
            )
            
            CustomItemServiceManager.getBlockItemModelPaths().forEach { (id, path) ->
                try {
                    val file = File(ResourcePackBuilder.PACK_DIR, "assets/nova/textures/waila_generated/${id.namespace}/${id.name}.png")
                    file.parentFile.mkdirs()
                    renderer.renderModelToFile(path.toString(), file)
                    addFontEntry(id.toString(), ResourcePath("nova", "waila_generated/${id.namespace}/${id.name}.png"))
                    LOGGER.info("Rendered $id ($path)")
                } catch (e: Exception) {
                    LOGGER.log(Level.WARNING, "Failed to render $id ($path) ", e)
                }
            }
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to render WAILA textures for custom item services. (Misconfigured base packs?)", e)
        }
    }
    
    private fun writeHardcodedTextures() {
        fun copyMCTexture(path: ResourcePath): ResourcePath {
            val from = File(ResourcePackBuilder.MCASSETS_DIR, "assets/${path.namespace}/textures/${path.path}")
            val name = path.path.substringAfterLast('/')
            val to = File(ResourcePackBuilder.PACK_DIR, "assets/nova/textures/waila_generated/$name")
            from.copyTo(to, overwrite = true)
            
            return ResourcePath("nova", "waila_generated/$name")
        }
        
        MATERIAL_TEXTURES.forEach { (material, texture) ->
            val name = material.name.lowercase()
            val path = ResourcePath.of((texture ?: "block/$name") + ".png")
            addFontEntry("minecraft:$name", copyMCTexture(path))
        }
        
        TEXTURES.forEach {
            addFontEntry("minecraft:$it", copyMCTexture(ResourcePath("minecraft", "block/$it.png")))
        }
    }
    
    override fun addFromPack(pack: AssetPack) {
        val wailaDir = File(ResourcePackBuilder.ASSETS_DIR, "${pack.namespace}/textures/waila/")
        
        if (!wailaDir.exists())
            return
        if (!WAILA_ENABLED) {
            wailaDir.deleteRecursively()
            return
        }
        
        wailaDir.walkTopDown().forEach { file ->
            if (file.isDirectory || !file.extension.equals("png", true))
                return@forEach
            
            val idNamespace = pack.namespace.takeUnless { it == "nova" } ?: "minecraft" // all textures form "nova" asset pack are for minecraft blocks
            val id = "$idNamespace:${file.nameWithoutExtension}"
            val path = ResourcePath(pack.namespace, "waila/${file.name}")
            
            addFontEntry(id, path)
        }
    }
    
    override fun createFontData(id: Int, char: Char, path: ResourcePath): WailaIconData =
        WailaIconData("nova:waila_textures_$id", char, path, getWidth(SIZE, path))
    
    class WailaIconData(font: String, char: Char, path: ResourcePath, width: Int) : FontData<FontChar>(font, char, path, width) {
        override val height = SIZE
        override val ascent = ASCENT
        override fun toFontInfo(): FontChar = FontChar(font, char, width)
    }
    
}