package xyz.xenondevs.nova.resources.builder.task

import net.kyori.adventure.key.Key
import org.bukkit.block.BlockType
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.AssetPack
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
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
private val MATERIAL_TEXTURES: Map<BlockType, String> = mapOf(
    // doors
    BlockType.IRON_DOOR to "item/iron_door",
    BlockType.OAK_DOOR to "item/oak_door",
    BlockType.SPRUCE_DOOR to "item/spruce_door",
    BlockType.BIRCH_DOOR to "item/birch_door",
    BlockType.JUNGLE_DOOR to "item/jungle_door",
    BlockType.ACACIA_DOOR to "item/acacia_door",
    BlockType.DARK_OAK_DOOR to "item/dark_oak_door",
    BlockType.MANGROVE_DOOR to "item/mangrove_door",
    BlockType.BAMBOO_DOOR to "item/bamboo_door",
    BlockType.CRIMSON_DOOR to "item/crimson_door",
    BlockType.WARPED_DOOR to "item/warped_door",
    BlockType.COPPER_DOOR to "item/copper_door",
    BlockType.WEATHERED_COPPER_DOOR to "item/weathered_copper_door",
    BlockType.OXIDIZED_COPPER_DOOR to "item/oxidized_copper_door",
    BlockType.EXPOSED_COPPER_DOOR to "item/exposed_copper_door",
    BlockType.WAXED_COPPER_DOOR to "item/copper_door",
    BlockType.WAXED_WEATHERED_COPPER_DOOR to "item/weathered_copper_door",
    BlockType.WAXED_OXIDIZED_COPPER_DOOR to "item/oxidized_copper_door",
    BlockType.WAXED_EXPOSED_COPPER_DOOR to "item/exposed_copper_door",
    BlockType.PALE_OAK_DOOR to "item/pale_oak_door",
    
    // signs
    BlockType.OAK_SIGN to "item/oak_sign",
    BlockType.SPRUCE_SIGN to "item/spruce_sign",
    BlockType.BIRCH_SIGN to "item/birch_sign",
    BlockType.JUNGLE_SIGN to "item/jungle_sign",
    BlockType.ACACIA_SIGN to "item/acacia_sign",
    BlockType.DARK_OAK_SIGN to "item/dark_oak_sign",
    BlockType.MANGROVE_SIGN to "item/mangrove_sign",
    BlockType.BAMBOO_SIGN to "item/bamboo_sign",
    BlockType.CRIMSON_SIGN to "item/crimson_sign",
    BlockType.WARPED_SIGN to "item/warped_sign",
    BlockType.BAMBOO_SIGN to "item/bamboo_sign",
    BlockType.CHERRY_SIGN to "item/cherry_sign",
    BlockType.PALE_OAK_SIGN to "item/pale_oak_sign",
    
    // hanging signs
    BlockType.OAK_HANGING_SIGN to "item/oak_hanging_sign",
    BlockType.SPRUCE_HANGING_SIGN to "item/spruce_hanging_sign",
    BlockType.BIRCH_HANGING_SIGN to "item/birch_hanging_sign",
    BlockType.JUNGLE_HANGING_SIGN to "item/jungle_hanging_sign",
    BlockType.ACACIA_HANGING_SIGN to "item/acacia_hanging_sign",
    BlockType.DARK_OAK_HANGING_SIGN to "item/dark_oak_hanging_sign",
    BlockType.MANGROVE_HANGING_SIGN to "item/mangrove_hanging_sign",
    BlockType.BAMBOO_HANGING_SIGN to "item/bamboo_hanging_sign",
    BlockType.CRIMSON_HANGING_SIGN to "item/crimson_hanging_sign",
    BlockType.WARPED_HANGING_SIGN to "item/warped_hanging_sign",
    BlockType.BAMBOO_HANGING_SIGN to "item/bamboo_hanging_sign",
    BlockType.CHERRY_HANGING_SIGN to "item/cherry_hanging_sign",
    BlockType.PALE_OAK_HANGING_SIGN to "item/pale_oak_hanging_sign",
    
    // foliage
    BlockType.OAK_SAPLING to "block/oak_sapling",
    BlockType.SPRUCE_SAPLING to "block/spruce_sapling",
    BlockType.BIRCH_SAPLING to "block/birch_sapling",
    BlockType.JUNGLE_SAPLING to "block/jungle_sapling",
    BlockType.ACACIA_SAPLING to "block/acacia_sapling",
    BlockType.DARK_OAK_SAPLING to "block/dark_oak_sapling",
    BlockType.CHERRY_SAPLING to "block/cherry_sapling",
    BlockType.MANGROVE_PROPAGULE to "block/mangrove_propagule",
    BlockType.BAMBOO_SAPLING to "block/bamboo_stage0",
    BlockType.PALE_OAK_SAPLING to "block/pale_oak_sapling",
    BlockType.DEAD_BUSH to "block/dead_bush",
    BlockType.SEAGRASS to "block/seagrass",
    BlockType.SEA_PICKLE to "block/sea_pickle",
    BlockType.CRIMSON_ROOTS to "block/crimson_roots",
    BlockType.WARPED_ROOTS to "block/warped_roots",
    BlockType.NETHER_SPROUTS to "block/nether_sprouts",
    BlockType.WEEPING_VINES to "block/weeping_vines",
    BlockType.TWISTING_VINES to "block/twisting_vines",
    BlockType.HANGING_ROOTS to "block/hanging_roots",
    BlockType.GLOW_LICHEN to "block/glow_lichen",
    BlockType.SCULK_VEIN to "block/sculk_vein",
    BlockType.SUGAR_CANE to "block/sugar_cane",
    BlockType.PUMPKIN_STEM to "item/pumpkin_seeds",
    BlockType.MELON_STEM to "item/melon_seeds",
    BlockType.PALE_HANGING_MOSS to "block/pale_hanging_moss",
    BlockType.FIREFLY_BUSH to "block/firefly_bush",
    BlockType.SHORT_DRY_GRASS to "block/short_dry_grass",
    BlockType.TALL_DRY_GRASS to "block/tall_dry_grass",
    
    // flowers
    BlockType.DANDELION to "block/dandelion",
    BlockType.POPPY to "block/poppy",
    BlockType.BLUE_ORCHID to "block/blue_orchid",
    BlockType.ALLIUM to "block/allium",
    BlockType.AZURE_BLUET to "block/azure_bluet",
    BlockType.RED_TULIP to "block/red_tulip",
    BlockType.ORANGE_TULIP to "block/orange_tulip",
    BlockType.WHITE_TULIP to "block/white_tulip",
    BlockType.PINK_TULIP to "block/pink_tulip",
    BlockType.OXEYE_DAISY to "block/oxeye_daisy",
    BlockType.LILY_OF_THE_VALLEY to "block/lily_of_the_valley",
    BlockType.WITHER_ROSE to "block/wither_rose",
    BlockType.CORNFLOWER to "block/cornflower",
    BlockType.SUNFLOWER to "block/sunflower_front",
    BlockType.LILAC to "block/lilac_top",
    BlockType.ROSE_BUSH to "block/rose_bush_top",
    BlockType.PEONY to "block/peony_top",
    BlockType.TORCHFLOWER to "block/torchflower",
    BlockType.PINK_PETALS to "item/pink_petals",
    BlockType.PITCHER_CROP to "item/pitcher_plant",
    BlockType.PITCHER_PLANT to "item/pitcher_plant",
    BlockType.OPEN_EYEBLOSSOM to "block/open_eyeblossom",
    BlockType.CLOSED_EYEBLOSSOM to "block/closed_eyeblossom",
    BlockType.CACTUS_FLOWER to "block/cactus_flower",
    BlockType.WILDFLOWERS to "block/wildflowers",
    
    // mushrooms
    BlockType.BROWN_MUSHROOM to "block/brown_mushroom",
    BlockType.RED_MUSHROOM to "block/red_mushroom",
    BlockType.CRIMSON_FUNGUS to "block/crimson_fungus",
    BlockType.WARPED_FUNGUS to "block/warped_fungus",
    
    // coral
    BlockType.TUBE_CORAL to "block/tube_coral",
    BlockType.BRAIN_CORAL to "block/brain_coral",
    BlockType.BUBBLE_CORAL to "block/bubble_coral",
    BlockType.FIRE_CORAL to "block/fire_coral",
    BlockType.HORN_CORAL to "block/horn_coral",
    BlockType.TUBE_CORAL_FAN to "block/tube_coral_fan",
    BlockType.BRAIN_CORAL_FAN to "block/brain_coral_fan",
    BlockType.BUBBLE_CORAL_FAN to "block/bubble_coral_fan",
    BlockType.FIRE_CORAL_FAN to "block/fire_coral_fan",
    BlockType.HORN_CORAL_FAN to "block/horn_coral_fan",
    BlockType.DEAD_BUBBLE_CORAL to "block/dead_bubble_coral",
    BlockType.DEAD_BRAIN_CORAL to "block/dead_brain_coral",
    BlockType.DEAD_FIRE_CORAL to "block/dead_fire_coral",
    BlockType.DEAD_HORN_CORAL to "block/dead_horn_coral",
    BlockType.DEAD_TUBE_CORAL to "block/dead_tube_coral",
    BlockType.DEAD_TUBE_CORAL_FAN to "block/dead_tube_coral_fan",
    BlockType.DEAD_BRAIN_CORAL_FAN to "block/dead_brain_coral_fan",
    BlockType.DEAD_BUBBLE_CORAL_FAN to "block/dead_bubble_coral_fan",
    BlockType.DEAD_FIRE_CORAL_FAN to "block/dead_fire_coral_fan",
    BlockType.DEAD_HORN_CORAL_FAN to "block/dead_horn_coral_fan",
    
    // glass pane
    BlockType.GLASS_PANE to "block/glass",
    BlockType.WHITE_STAINED_GLASS_PANE to "block/white_stained_glass",
    BlockType.ORANGE_STAINED_GLASS_PANE to "block/orange_stained_glass",
    BlockType.MAGENTA_STAINED_GLASS_PANE to "block/magenta_stained_glass",
    BlockType.LIGHT_BLUE_STAINED_GLASS_PANE to "block/light_blue_stained_glass",
    BlockType.YELLOW_STAINED_GLASS_PANE to "block/yellow_stained_glass",
    BlockType.LIME_STAINED_GLASS_PANE to "block/lime_stained_glass",
    BlockType.PINK_STAINED_GLASS_PANE to "block/pink_stained_glass",
    BlockType.GRAY_STAINED_GLASS_PANE to "block/gray_stained_glass",
    BlockType.LIGHT_GRAY_STAINED_GLASS_PANE to "block/light_gray_stained_glass",
    BlockType.CYAN_STAINED_GLASS_PANE to "block/cyan_stained_glass",
    BlockType.PURPLE_STAINED_GLASS_PANE to "block/purple_stained_glass",
    BlockType.BLUE_STAINED_GLASS_PANE to "block/blue_stained_glass",
    BlockType.BROWN_STAINED_GLASS_PANE to "block/brown_stained_glass",
    BlockType.GREEN_STAINED_GLASS_PANE to "block/green_stained_glass",
    BlockType.RED_STAINED_GLASS_PANE to "block/red_stained_glass",
    BlockType.BLACK_STAINED_GLASS_PANE to "block/black_stained_glass",
    
    // amethyst
    BlockType.SMALL_AMETHYST_BUD to "block/small_amethyst_bud",
    BlockType.MEDIUM_AMETHYST_BUD to "block/medium_amethyst_bud",
    BlockType.LARGE_AMETHYST_BUD to "block/large_amethyst_bud",
    BlockType.AMETHYST_CLUSTER to "block/amethyst_cluster",
    
    // misc
    BlockType.TORCH to "block/torch",
    BlockType.REDSTONE_TORCH to "block/redstone_torch",
    BlockType.SOUL_TORCH to "block/soul_torch",
    BlockType.COPPER_TORCH to "block/copper_torch",
    BlockType.LADDER to "block/ladder",
    BlockType.IRON_CHAIN to "item/iron_chain",
    BlockType.COPPER_CHAIN to "item/copper_chain",
    BlockType.EXPOSED_COPPER_CHAIN to "item/exposed_copper_chain",
    BlockType.WEATHERED_COPPER_CHAIN to "item/weathered_copper_chain",
    BlockType.OXIDIZED_COPPER_CHAIN to "item/oxidized_copper_chain",
    BlockType.WAXED_COPPER_CHAIN to "item/copper_chain",
    BlockType.WAXED_EXPOSED_COPPER_CHAIN to "item/exposed_copper_chain",
    BlockType.WAXED_WEATHERED_COPPER_CHAIN to "item/weathered_copper_chain",
    BlockType.WAXED_OXIDIZED_COPPER_CHAIN to "item/oxidized_copper_chain",
    BlockType.IRON_BARS to "block/iron_bars",
    BlockType.COPPER_BARS to "block/copper_bars",
    BlockType.EXPOSED_COPPER_BARS to "block/exposed_copper_bars",
    BlockType.WEATHERED_COPPER_BARS to "block/weathered_copper_bars",
    BlockType.OXIDIZED_COPPER_BARS to "block/oxidized_copper_bars",
    BlockType.WAXED_COPPER_BARS to "block/copper_bars",
    BlockType.WAXED_EXPOSED_COPPER_BARS to "block/exposed_copper_bars",
    BlockType.WAXED_WEATHERED_COPPER_BARS to "block/weathered_copper_bars",
    BlockType.WAXED_OXIDIZED_COPPER_BARS to "block/oxidized_copper_bars",
    BlockType.LEVER to "block/lever",
    BlockType.TRIPWIRE_HOOK to "block/tripwire_hook",
    BlockType.FROGSPAWN to "block/frogspawn",
    BlockType.TRIPWIRE to "item/string",
    BlockType.COBWEB to "block/cobweb",
    BlockType.BARRIER to "item/barrier",
    BlockType.STRUCTURE_VOID to "item/structure_void",
    BlockType.POINTED_DRIPSTONE to "item/pointed_dripstone",
    BlockType.SULFUR_SPIKE to "item/sulfur_spike",
    BlockType.RAIL to "block/rail",
    BlockType.REDSTONE_WIRE to "item/redstone",
    BlockType.BELL to "item/bell"
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

private const val SIZE = 24
private const val ASCENT = -4

/**
 * Writes WAILA assets to the resource pack.
 */
class WailaTask(
    builder: ResourcePackBuilder
) : CustomFontContent(
    builder,
    "nova:waila_textures_%s",
    1..18
), PackTask {
    
    override val stage = BuildStage.POST_WORLD
    override val runsBefore = setOf(MovedFontContent.Write::class, FontContent.Write::class, TextureIconContent.Write::class)
    
    override suspend fun run() {
        if (!WailaManager.ENABLED)
            return
        
        builder.getBuildData<TextureIconContent>().addIcons(
            "minecraft:item/wooden_sword", "minecraft:item/wooden_shovel", "minecraft:item/wooden_pickaxe", "minecraft:item/wooden_axe", "minecraft:item/wooden_hoe",
            "minecraft:item/stone_sword", "minecraft:item/stone_shovel", "minecraft:item/stone_pickaxe", "minecraft:item/stone_axe", "minecraft:item/stone_hoe",
            "minecraft:item/copper_sword", "minecraft:item/copper_shovel", "minecraft:item/copper_pickaxe", "minecraft:item/copper_axe", "minecraft:item/copper_hoe",
            "minecraft:item/iron_sword", "minecraft:item/iron_shovel", "minecraft:item/iron_pickaxe", "minecraft:item/iron_axe", "minecraft:item/iron_hoe",
            "minecraft:item/golden_sword", "minecraft:item/golden_shovel", "minecraft:item/golden_pickaxe", "minecraft:item/golden_axe", "minecraft:item/golden_hoe",
            "minecraft:item/diamond_sword", "minecraft:item/diamond_shovel", "minecraft:item/diamond_pickaxe", "minecraft:item/diamond_axe", "minecraft:item/diamond_hoe",
            "minecraft:item/netherite_sword", "minecraft:item/netherite_shovel", "minecraft:item/netherite_pickaxe", "minecraft:item/netherite_axe", "minecraft:item/netherite_hoe",
            "minecraft:item/shears",
        )
        
        if (WailaManager.ENABLED) {
            writeHardcodedTextures()
            builder.assetPacks.forEach(::writePackTextures)
            renderCustomItemServiceBlocks()
            ResourceLookups.wailaData = fontCharLookup
        }
    }
    
    private fun renderCustomItemServiceBlocks() {
        var count = 0
        try {
            val renderer = MinecraftModelRenderer(
                512, 512,
                128, 128,
                listOf(builder.resolveVanilla(""), builder.resolve("")),
                true
            )
            
            CustomItemServiceManager.getBlockItemModelPaths().forEach { [id, path] ->
                try {
                    val file = builder.resolve("assets/nova/textures/waila_generated/${id.namespace()}/${id.value()}.png")
                    file.parent.createDirectories()
                    renderer.renderModelToFile(path.asString(), file)
                    addEntry(id, ResourcePath(ResourceType.FontTexture, "nova", "waila_generated/${id.namespace()}/${id.value()}.png"), SIZE, ASCENT)
                    count++
                } catch (e: Exception) {
                    builder.logger.warn("Failed to render ${id.asString()} (${path.asString()}) ", e)
                }
            }
        } catch (e: Exception) {
            builder.logger.error("Failed to render WAILA textures for custom item services. (Misconfigured base packs?)", e)
        } finally {
            builder.logger.info("Rendered $count WAILA textures")
        }
    }
    
    private fun writeHardcodedTextures() {
        fun copyMCTexture(from: ResourcePath<ResourceType.PngFile>): ResourcePath<ResourceType.FontTexture> {
            val name = from.path.substringAfterLast('/')
            val to = ResourcePath(ResourceType.FontTexture, "nova", "waila_generated/$name.png")
            
            val fromFile = builder.resolveVanilla(from)
            val toFile = builder.resolve(to)
            toFile.parent.createDirectories()
            fromFile.copyTo(toFile, overwrite = true)
            
            return to
        }
        
        MATERIAL_TEXTURES.forEach { [type, texture] ->
            val path = ResourcePath.of(ResourceType.FontTexture, "$texture.png")
            addEntry(type.key, copyMCTexture(path), SIZE, ASCENT)
        }
        
        TEXTURES.forEach {
            addEntry(Key.key("minecraft", it), copyMCTexture(ResourcePath(ResourceType.Texture, "minecraft", "block/$it")), SIZE, ASCENT)
        }
    }
    
    private fun writePackTextures(pack: AssetPack) {
        if (!WailaManager.ENABLED)
            return
        
        val wailaDir = builder.resolve("assets/${pack.namespace}/textures/waila/")
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