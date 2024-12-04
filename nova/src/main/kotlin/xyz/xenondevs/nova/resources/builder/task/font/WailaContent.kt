package xyz.xenondevs.nova.resources.builder.task.font

import net.kyori.adventure.key.Key
import org.bukkit.Material
import xyz.xenondevs.commons.collections.enumMapOf
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.AssetPack
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder.Companion.ASSETS_DIR
import xyz.xenondevs.nova.resources.builder.task.BuildStage
import xyz.xenondevs.nova.resources.builder.task.PackTask
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.renderer.MinecraftModelRenderer
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk

//<editor-fold desc="Hardcoded Textures", defaultstate="collapsed">
private val MATERIAL_TEXTURES: Map<Material, String> = enumMapOf(
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
    Material.COPPER_DOOR to "item/copper_door",
    Material.WEATHERED_COPPER_DOOR to "item/weathered_copper_door",
    Material.OXIDIZED_COPPER_DOOR to "item/oxidized_copper_door",
    Material.EXPOSED_COPPER_DOOR to "item/exposed_copper_door",
    Material.WAXED_COPPER_DOOR to "item/copper_door",
    Material.WAXED_WEATHERED_COPPER_DOOR to "item/weathered_copper_door",
    Material.WAXED_OXIDIZED_COPPER_DOOR to "item/oxidized_copper_door",
    Material.WAXED_EXPOSED_COPPER_DOOR to "item/exposed_copper_door",
    
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
    Material.CHERRY_SIGN to "item/cherry_sign",
    
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
    Material.CHERRY_HANGING_SIGN to "item/cherry_hanging_sign",
    
    // foliage
    Material.OAK_SAPLING to "block/oak_sapling",
    Material.SPRUCE_SAPLING to "block/spruce_sapling",
    Material.BIRCH_SAPLING to "block/birch_sapling",
    Material.JUNGLE_SAPLING to "block/jungle_sapling",
    Material.ACACIA_SAPLING to "block/acacia_sapling",
    Material.DARK_OAK_SAPLING to "block/dark_oak_sapling",
    Material.CHERRY_SAPLING to "block/cherry_sapling",
    Material.MANGROVE_PROPAGULE to "block/mangrove_propagule",
    Material.BAMBOO_SAPLING to "block/bamboo_stage0",
    Material.DEAD_BUSH to "block/dead_bush",
    Material.SEAGRASS to "block/seagrass",
    Material.SEA_PICKLE to "block/sea_pickle",
    Material.CRIMSON_ROOTS to "block/crimson_roots",
    Material.WARPED_ROOTS to "block/warped_roots",
    Material.NETHER_SPROUTS to "block/nether_sprouts",
    Material.WEEPING_VINES to "block/weeping_vines",
    Material.TWISTING_VINES to "block/twisting_vines",
    Material.HANGING_ROOTS to "block/hanging_roots",
    Material.GLOW_LICHEN to "block/glow_lichen",
    Material.SCULK_VEIN to "block/sculk_vein",
    Material.SUGAR_CANE to "block/sugar_cane",
    Material.PUMPKIN_STEM to "item/pumpkin_seeds",
    Material.MELON_STEM to "item/melon_seeds",
    
    // flowers
    Material.DANDELION to "block/dandelion",
    Material.POPPY to "block/poppy",
    Material.BLUE_ORCHID to "block/blue_orchid",
    Material.ALLIUM to "block/allium",
    Material.AZURE_BLUET to "block/azure_bluet",
    Material.RED_TULIP to "block/red_tulip",
    Material.ORANGE_TULIP to "block/orange_tulip",
    Material.WHITE_TULIP to "block/white_tulip",
    Material.PINK_TULIP to "block/pink_tulip",
    Material.OXEYE_DAISY to "block/oxeye_daisy",
    Material.LILY_OF_THE_VALLEY to "block/lily_of_the_valley",
    Material.WITHER_ROSE to "block/wither_rose",
    Material.CORNFLOWER to "block/cornflower",
    Material.SUNFLOWER to "block/sunflower_front",
    Material.LILAC to "block/lilac_top",
    Material.ROSE_BUSH to "block/rose_bush_top",
    Material.PEONY to "block/peony_top",
    Material.TORCHFLOWER to "block/torchflower",
    Material.PINK_PETALS to "item/pink_petals",
    Material.PITCHER_CROP to "item/pitcher_plant",
    Material.PITCHER_PLANT to "item/pitcher_plant",
    
    // mushrooms
    Material.BROWN_MUSHROOM to "block/brown_mushroom",
    Material.RED_MUSHROOM to "block/red_mushroom",
    Material.CRIMSON_FUNGUS to "block/crimson_fungus",
    Material.WARPED_FUNGUS to "block/warped_fungus",
    
    // coral
    Material.TUBE_CORAL to "block/tube_coral",
    Material.BRAIN_CORAL to "block/brain_coral",
    Material.BUBBLE_CORAL to "block/bubble_coral",
    Material.FIRE_CORAL to "block/fire_coral",
    Material.HORN_CORAL to "block/horn_coral",
    Material.TUBE_CORAL_FAN to "block/tube_coral_fan",
    Material.BRAIN_CORAL_FAN to "block/brain_coral_fan",
    Material.BUBBLE_CORAL_FAN to "block/bubble_coral_fan",
    Material.FIRE_CORAL_FAN to "block/fire_coral_fan",
    Material.HORN_CORAL_FAN to "block/horn_coral_fan",
    Material.DEAD_BUBBLE_CORAL to "block/dead_bubble_coral",
    Material.DEAD_BRAIN_CORAL to "block/dead_brain_coral",
    Material.DEAD_FIRE_CORAL to "block/dead_fire_coral",
    Material.DEAD_HORN_CORAL to "block/dead_horn_coral",
    Material.DEAD_TUBE_CORAL to "block/dead_tube_coral",
    Material.DEAD_TUBE_CORAL_FAN to "block/dead_tube_coral_fan",
    Material.DEAD_BRAIN_CORAL_FAN to "block/dead_brain_coral_fan",
    Material.DEAD_BUBBLE_CORAL_FAN to "block/dead_bubble_coral_fan",
    Material.DEAD_FIRE_CORAL_FAN to "block/dead_fire_coral_fan",
    Material.DEAD_HORN_CORAL_FAN to "block/dead_horn_coral_fan",
    
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
    Material.SMALL_AMETHYST_BUD to "block/small_amethyst_bud",
    Material.MEDIUM_AMETHYST_BUD to "block/medium_amethyst_bud",
    Material.LARGE_AMETHYST_BUD to "block/large_amethyst_bud",
    Material.AMETHYST_CLUSTER to "block/amethyst_cluster",
    
    // misc
    Material.TORCH to "block/torch",
    Material.REDSTONE_TORCH to "block/redstone_torch",
    Material.SOUL_TORCH to "block/soul_torch",
    Material.LADDER to "block/ladder",
    Material.CHAIN to "item/chain",
    Material.IRON_BARS to "block/iron_bars",
    Material.LEVER to "block/lever",
    Material.TRIPWIRE_HOOK to "block/tripwire_hook",
    Material.FROGSPAWN to "block/frogspawn",
    Material.STRING to "item/string",
    Material.COBWEB to "block/cobweb",
    Material.BARRIER to "item/barrier",
    Material.STRUCTURE_VOID to "item/structure_void",
    Material.POINTED_DRIPSTONE to "item/pointed_dripstone",
    Material.RAIL to "block/rail",
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
    "torchflower_crop_stage0", "torchflower_crop_stage1",
    "activator_rail", "activator_rail_on", "detector_rail", "detector_rail_on", "powered_rail", "powered_rail_on"
)
//</editor-fold>

private const val SIZE = 32
private const val ASCENT = -4

class WailaContent internal constructor(
    builder: ResourcePackBuilder
) : CustomFontContent(
    builder,
    "nova:waila_textures_%s",
    true
) {
    
    @PackTask(stage = BuildStage.POST_WORLD, runBefore = ["FontContent#write"])
    private fun write() {
        if (WailaManager.ENABLED) {
            builder.getHolder<MovedFontContent>().requestMovedFonts(ResourcePath(ResourceType.Font, "nova", "waila"), 1..19)
            writeHardcodedTextures()
            builder.assetPacks.forEach(::writePackTextures)
            renderCustomItemServiceBlocks()
            ResourceLookups.WAILA_DATA_LOOKUP.set(fontCharLookup)
        }
    }
    
    private fun renderCustomItemServiceBlocks() {
        var count = 0
        try {
            val renderer = MinecraftModelRenderer(
                512, 512,
                128, 128,
                listOf(ResourcePackBuilder.MCASSETS_DIR, ResourcePackBuilder.PACK_DIR),
                true
            )
            
            CustomItemServiceManager.getBlockItemModelPaths().forEach { (id, path) ->
                try {
                    val file = ResourcePackBuilder.PACK_DIR.resolve("assets/nova/textures/waila_generated/${id.namespace()}/${id.value()}.png")
                    file.parent.createDirectories()
                    renderer.renderModelToFile(path.toString(), file)
                    addEntry(id, ResourcePath(ResourceType.FontTexture, "nova", "waila_generated/${id.namespace()}/${id.value()}.png"), SIZE, ASCENT)
                    count++
                } catch (e: Exception) {
                    LOGGER.warn("Failed to render $id ($path) ", e)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to render WAILA textures for custom item services. (Misconfigured base packs?)", e)
        } finally {
            LOGGER.info("Rendered $count WAILA textures")
        }
    }
    
    private fun writeHardcodedTextures() {
        fun copyMCTexture(from: ResourcePath<ResourceType.Texture>): ResourcePath<ResourceType.FontTexture> {
            val name = from.path.substringAfterLast('/')
            val to = ResourcePath(ResourceType.FontTexture, "nova", "waila_generated/$name.png")
            
            val fromFile = builder.resolveVanilla(from)
            val toFile = builder.resolve(to)
            toFile.parent.createDirectories()
            fromFile.copyTo(toFile, overwrite = true)
            
            return to
        }
        
        MATERIAL_TEXTURES.forEach { (material, texture) ->
            val name = material.name.lowercase()
            val path = ResourcePath.of(ResourceType.FontTexture, "$texture.png")
            addEntry(Key.key("minecraft", name), copyMCTexture(path), SIZE, ASCENT)
        }
        
        TEXTURES.forEach {
            addEntry(Key.key("minecraft", it), copyMCTexture(ResourcePath(ResourceType.Texture, "minecraft", "block/$it")), SIZE, ASCENT)
        }
    }
    
    private fun writePackTextures(pack: AssetPack) {
        if (!WailaManager.ENABLED)
            return
        
        val wailaDir = ASSETS_DIR.resolve("${pack.namespace}/textures/waila/")
        if (!wailaDir.exists())
            return
        
        wailaDir.walk().forEach { file ->
            if (file.isDirectory() || !file.extension.equals("png", true))
                return@forEach
            
            val idNamespace = pack.namespace.takeUnless { it == "nova" } ?: "minecraft" // all textures form "nova" asset pack are for minecraft blocks
            addEntry(
                Key.key(idNamespace, file.nameWithoutExtension),
                ResourcePath(ResourceType.FontTexture, pack.namespace, "waila/${file.name}"),
                SIZE, ASCENT
            )
        }
    }
    
}