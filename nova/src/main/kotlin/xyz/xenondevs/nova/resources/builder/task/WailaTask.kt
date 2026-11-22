package xyz.xenondevs.nova.resources.builder.task

import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.Font
import xyz.xenondevs.nova.resources.builder.font.provider.bitmap.BitmapProvider
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.ui.waila.WailaIcon
import xyz.xenondevs.nova.ui.waila.WailaIconProvider
import xyz.xenondevs.nova.ui.waila.WailaToolIconProvider
import xyz.xenondevs.nova.ui.waila.overlay.WailaManager
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.toPropertyStringMap
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.tessera.DirectoryPack
import xyz.xenondevs.tessera.NioPack
import xyz.xenondevs.tessera.RenderRequest
import xyz.xenondevs.tessera.RenderResult
import xyz.xenondevs.tessera.TesseraRenderer
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeBytes
import kotlin.time.measureTime

private const val SIZE = 24
private const val ASCENT = -4

private val RENDER_SCALE by MAIN_CONFIG.entry(4, "waila", "render_scale")

private fun WailaIcon.Framing.toTessera(): RenderRequest.Framing = when (this) {
    Gui -> RenderRequest.Framing.Gui()
    is Fit -> RenderRequest.Framing.Fit(margin)
}

private fun WailaIcon.toRenderRequest(targetSize: Int): RenderRequest = when (this) {
    is WailaIcon.Item -> RenderRequest.Item(
        itemType.key.asString(),
        targetSize,
        PNG,
        framing.toTessera(),
        RenderRequest.FlatResolution.NATIVE
    )
    
    is WailaIcon.BlockState -> RenderRequest.BlockState(
        blockState.blockType.key.asString(),
        blockState.nmsBlockState
            .toPropertyStringMap()
            .entries
            .joinToString(",") { [k, v] -> "$k=$v" },
        targetSize,
        PNG,
        framing.toTessera(),
    )
    
    is WailaIcon.Model -> RenderRequest.Model(
        path.asString(),
        targetSize,
        PNG,
        framing.toTessera(),
    )
    
    is WailaIcon.Texture -> throw IllegalArgumentException("Texture icons are not rendered")
}

private fun <K> Map<K, WailaIcon>.toChars(chars: Map<WailaIcon, FontChar>): Map<K, FontChar> =
    entries.mapNotNull { [key, icon] -> chars[icon]?.let { key to it } }.toMap()

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
    override val runsAfter = setOf(ItemModelContent.Write::class)
    override val runsBefore = setOf(MovedFontContent.Write::class, FontContent.Write::class, TextureIconContent.Write::class)
    
    private val fontContent by builder.getBuildDataLazily<FontContent>()
    private val movedFontContent by builder.getBuildDataLazily<MovedFontContent>()
    
    private val font = Font(ResourcePath(ResourceType.Font, "nova", "waila_textures"))
    private var codePoint = 0
    private var textureId = 0
    
    override suspend fun run() {
        if (!WailaManager.ENABLED)
            return
        
        requestToolIcons()
        
        fontContent += font
        movedFontContent.requestMovedFonts(font.id, 1..18)
        
        val blockIcons = resolveBlockIcons()
        val customItemServiceIcons = CustomItemServiceManager.getWailaIcons()
        val chars = renderIcons(blockIcons.values.toHashSet() + customItemServiceIcons.values)
        
        ResourceLookups.wailaIcon = blockIcons.toChars(chars)
        ResourceLookups.customItemServiceWailaIcon = customItemServiceIcons.toChars(chars)
    }
    
    private fun requestToolIcons() {
        val icons = WailaToolIconProvider.TOOL_ICON_IDS.get().values
            .flatMapTo(HashSet()) { it }
            .map { ResourcePath.of(ResourceType.Texture, it) }
        builder.getBuildData<TextureIconContent>().addIcons(icons)
    }
    
    private fun resolveBlockIcons(): Map<BlockData, WailaIcon> {
        val icons = HashMap<BlockData, WailaIcon>()
        NovaRegistries.WAILA_ICON_PROVIDER.entrySet.get()
            .flatMap { it.blocks.get().map { block -> block to it } }
            .groupBy({ [k, _] -> k }, { [_, v] -> v })
            .mapValues { [_, providers] -> providers.sortedByDescending { it.priority } }
            .forEach { [blockType, providers] ->
                @Suppress("UNCHECKED_CAST")
                providers as List<WailaIconProvider<BlockData>>
                
                for (blockState in blockType.createBlockDataStates()) {
                    icons[blockState] = providers
                        .firstNotNullOfOrNull { provider -> provider.getIcon(blockState) }
                        ?: continue
                }
            }
        
        return icons
    }
    
    private fun renderIcons(icons: Set<WailaIcon>): Map<WailaIcon, FontChar> {
        val renderer = TesseraRenderer([
            NioPack(builder.resolve("")),
            DirectoryPack(builder.resolveVanilla("").toFile())
        ])
        
        val targetSize = SIZE * RENDER_SCALE
        val chars = HashMap<WailaIcon, FontChar>()
        
        val renderRequests = HashMap<RenderRequest, ArrayList<WailaIcon>>()
        for (icon in icons) {
            if (icon is WailaIcon.Texture) {
                chars[icon] = addEntry(icon.path.toType(ResourceType.FontTexture))
            } else {
                renderRequests.getOrPut(icon.toRenderRequest(targetSize), ::ArrayList) += icon
            }
        }
        
        val requests = renderRequests.keys.toList()
        val results: List<RenderResult>
        val time = measureTime { results = renderer.render(requests) }
        builder.logger.info("Rendered ${results.size} WAILA textures in $time")
        
        for ([request, result] in requests.zip(results)) {
            val char = when (result) {
                is RenderResult.Failure -> {
                    builder.logger.error("Failed to render WAILA texture for $request: ${result.error}")
                    continue
                }
                
                is RenderResult.Png -> {
                    val path = ResourcePath(ResourceType.FontTexture, "nova", "waila_rendered/${textureId++}.png")
                    builder.resolve(path).apply { createParentDirectories() }.writeBytes(result.png)
                    addEntry(path)
                }
                
                is RenderResult.Pixels -> error("Requested PNG, got pixels")
                
                is RenderResult.Empty -> continue
            }
            
            for (icon in renderRequests[request]!!)
                chars[icon] = char
        }
        
        return chars
    }
    
    private fun addEntry(img: ResourcePath<ResourceType.FontTexture>): FontChar {
        font.add(BitmapProvider.single(builder, img, ++codePoint, SIZE, ASCENT))
        return FontChar(font.id, codePoint)
    }
    
}