package xyz.xenondevs.nova.data.resources.builder.content.material

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.Material
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.ModelData
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.BlockSoundOverrides
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import xyz.xenondevs.nova.data.resources.builder.basepack.merger.ModelFileMerger
import xyz.xenondevs.nova.data.resources.builder.content.PackContent
import xyz.xenondevs.nova.data.resources.builder.content.material.info.BlockDirection
import xyz.xenondevs.nova.data.resources.builder.content.material.info.BlockModelType
import xyz.xenondevs.nova.data.resources.builder.content.material.info.ModelInformation
import xyz.xenondevs.nova.data.resources.builder.content.material.info.RegisteredMaterial
import xyz.xenondevs.nova.data.resources.builder.content.material.info.VanillaMaterialTypes
import xyz.xenondevs.nova.data.resources.model.blockstate.BlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.BlockStateConfigType
import xyz.xenondevs.nova.data.resources.model.data.ArmorStandBlockModelData
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.BlockStateBlockModelData
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.parseJson
import xyz.xenondevs.nova.util.mapToIntArray
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

private val USE_SOLID_BLOCKS by configReloadable { DEFAULT_CONFIG.getBoolean("resource_pack.generation.use_solid_blocks") }

internal class MaterialContent(
    private val basePacks: BasePacks,
    private val soundOverrides: BlockSoundOverrides
) : PackContent {
    
    override val stage = ResourcePackBuilder.BuildingStage.PRE_WORLD
    
    private val novaMaterials = HashMap<NamespacedId, RegisteredMaterial>()
    
    private val modelDataPosition = HashMap<Material, Int>()
    
    private val blockStatePosition = HashMap<BlockStateConfigType<*>, Int>()
    private val remainingBlockStates = HashMap<BlockStateConfigType<*>, Int>()
    
    override fun includePack(pack: AssetPack) {
        val materialsIndex = pack.materialsIndex ?: return
        
        materialsIndex.forEach { registeredMaterial ->
            novaMaterials[registeredMaterial.id] = registeredMaterial
            createDefaultModelFiles(pack, registeredMaterial.itemInfo)
        }
    }
    
    private fun createDefaultModelFiles(pack: AssetPack, info: ModelInformation) {
        info.models.forEach {
            val namespace = pack.namespace
            val file = ResourcePackBuilder.ASSETS_DIR.resolve("$namespace/models/${it.removePrefix("$namespace:")}.json")
            if (!file.exists())
                createDefaultModelFile(file, it)
        }
    }
    
    private fun createDefaultModelFile(file: Path, texturePath: String) {
        val modelObj = JsonObject()
        modelObj.addProperty("parent", "item/generated")
        modelObj.add(
            "textures",
            JsonObject().apply {
                addProperty("layer0", "item/empty") // this fixes issues with leather armor colors
                addProperty("layer1", texturePath)
            }
        )
        
        file.parent.createDirectories()
        file.writeText(GSON.toJson(modelObj))
    }
    
    @Suppress("ReplaceWithEnumMap")
    override fun write() {
        // the general lookup later passed to Resources
        val modelDataLookup = HashMap<NamespacedId, ModelData>()
        
        // stores the custom model id overrides, used to prevent duplicate registration of armor stand models and for writing to the respective file
        val customItemModels = HashMap<Material, HashMap<String, Int>>()
        // stores block state block models by their name and direction, used to prevent duplicate registration of solid blocks
        val blockStateModelsByName = HashMap<Pair<String, BlockDirection>, BlockStateConfig>()
        // stores block state block models by their type and id / direction, used for writing them to their respective files
        val blockStateModelsByType = HashMap<BlockStateConfigType<*>, HashMap<Pair<String, BlockDirection>, BlockStateConfig>>()
        
        // generate item models map
        novaMaterials.forEach { (id, regMat) ->
            val info = regMat.itemInfo
            // create map containing all ItemModelData instances for each vanilla material of this item
            val materialsMap: HashMap<Material, ItemModelData> = HashMap()
            modelDataLookup[id] = ModelData(materialsMap, null, regMat.armor)
            // register that item model under the required vanilla materials
            val materials = info.material?.let(::listOf) ?: VanillaMaterialTypes.MATERIALS
            materials.forEach { material ->
                val registeredModels = customItemModels.getOrPut(material, ::HashMap)
                val dataArray = info.models.mapToIntArray { model -> registeredModels.getOrPut(model) { getNextCustomModelData(material) } }
                materialsMap[material] = ItemModelData(info.id, material, dataArray)
            }
        }
        
        // generate block models map
        novaMaterials.entries
            .sortedByDescending { it.value.blockInfo.priority }
            .forEach { (id, regMat) ->
                val info = regMat.blockInfo
                val modelData = modelDataLookup[id]!!
                
                val blockModelData: BlockModelData
                if (getRemainingBlockStateIdAmount(info.type) < info.models.size) {
                    // If there are not enough block states left over for this block, use armor stands to display it
                    val material = VanillaMaterialTypes.DEFAULT_MATERIAL
                    val registeredModels = customItemModels.getOrPut(material, ::HashMap)
                    val dataArray = info.models.mapToIntArray { registeredModels.getOrPut(it) { getNextCustomModelData(material) } }
                    blockModelData = ArmorStandBlockModelData(id, info.hitboxType, dataArray)
                    
                    // note hitbox type as used material for sound overrides
                    soundOverrides.useMaterial(info.hitboxType)
                } else {
                    val configs = HashMap<BlockFace, ArrayList<BlockStateConfig>>()
                    info.models.forEach { model ->
                        info.directions.forEach { direction ->
                            val faceList = configs.getOrPut(direction.blockFace, ::ArrayList)
                            
                            val modelDirectionPair = model to direction
                            val blockConfig = blockStateModelsByName.getOrPut(modelDirectionPair) { getNextBlockConfig(info.type) }
                            blockStateModelsByType.getOrPut(blockConfig.type, ::HashMap)[modelDirectionPair] = blockConfig
                            
                            faceList += blockConfig
                            
                            // note block type as used material for sound overrides
                            soundOverrides.useMaterial(blockConfig.type.material)
                        }
                    }
                    
                    blockModelData = BlockStateBlockModelData(id, configs)
                }
                
                modelDataLookup[id] = modelData.copy(block = blockModelData)
            }
        
        // pass modelDataLookup to Resources
        Resources.updateModelDataLookup(modelDataLookup)
        
        // write item models
        customItemModels.forEach { (material, registeredModels) ->
            val (file, modelObj, overrides) = getModelFile(material)
            
            registeredModels
                .toList()
                .sortedBy { it.second }
                .forEach { (path, customModelData) ->
                    overrides.add(ModelFileMerger.createModelDataEntry(customModelData, path))
                }
            
            modelObj.add("overrides", ModelFileMerger.sortOverrides(overrides))
            
            file.parent.createDirectories()
            file.writeText(GSON.toJson(modelObj))
        }
        
        // write block models
        blockStateModelsByType.forEach { (type, registeredModels) ->
            val (file, mainObj, variants) = getBlockStateFile(type)
            
            registeredModels.forEach { (pair, cfg) ->
                val (path, direction) = pair
                val variant = JsonObject()
                variant.addProperty("model", path)
                variant.addProperty("x", direction.x)
                variant.addProperty("y", direction.y)
                variants.add(cfg.variantString, variant)
            }
            
            file.parent.createDirectories()
            file.writeText(GSON.toJson(mainObj))
        }
    }
    
    private fun getNextCustomModelData(material: Material): Int {
        var pos = modelDataPosition.getOrPut(material) { 0 } + 1
        
        val occupiedSet = basePacks.occupiedModelData[material]
        if (occupiedSet != null) {
            while (pos in occupiedSet) {
                pos++
            }
        }
        
        modelDataPosition[material] = pos
        
        return pos
    }
    
    private fun getNextBlockConfig(type: BlockModelType): BlockStateConfig {
        val configType = type.configTypes.first { it != null && getRemainingBlockStateIdAmount(it) > 0 }!!
        return getNextBlockConfig(configType)
    }
    
    private fun getNextBlockConfig(type: BlockStateConfigType<*>): BlockStateConfig {
        var pos = blockStatePosition.getOrPut(type) { -1 } + 1
        
        val occupiedSet = basePacks.occupiedSolidIds[type]
        val blockedSet = type.blockedIds
        
        while (pos in blockedSet || (occupiedSet != null && pos in occupiedSet)) {
            pos++
        }
        
        blockStatePosition[type] = pos
        remainingBlockStates[type] = remainingBlockStates[type]!! - 1
        
        check(pos < type.maxId) { "Id limit exceeded" }
        return type.of(pos)
    }
    
    private fun getRemainingBlockStateIdAmount(type: BlockModelType): Int {
        if (!USE_SOLID_BLOCKS) return 0
        return type.configTypes.sumOf { if (it != null) getRemainingBlockStateIdAmount(it) else 0 }
    }
    
    private fun getRemainingBlockStateIdAmount(type: BlockStateConfigType<*>): Int {
        return remainingBlockStates.getOrPut(type) {
            var count = 0
            
            val occupiedSet = basePacks.occupiedSolidIds[type]
            val blockedSet = type.blockedIds
            
            for (pos in 0..type.maxId) {
                if (pos in blockedSet || (occupiedSet != null && pos in occupiedSet)) continue
                count++
            }
            
            return@getOrPut count
        }
    }
    
    private fun getModelFile(material: Material): Triple<Path, JsonObject, JsonArray> {
        val path = "minecraft/models/item/${material.name.lowercase()}.json"
        val destFile = ResourcePackBuilder.ASSETS_DIR.resolve(path)
        val sourceFile = destFile.takeIf(Path::exists) ?: ResourcePackBuilder.MCASSETS_ASSETS_DIR.resolve(path)
        require(sourceFile.exists()) { "Source model file does not exist: $sourceFile" }
        
        val modelObj = sourceFile.parseJson() as JsonObject
        val overrides = (modelObj.get("overrides") as? JsonArray) ?: JsonArray().also { modelObj.add("overrides", it) }
        return Triple(destFile, modelObj, overrides)
    }
    
    private fun getBlockStateFile(type: BlockStateConfigType<*>): Triple<Path, JsonObject, JsonObject> {
        val file = ResourcePackBuilder.ASSETS_DIR.resolve("minecraft/blockstates/${type.fileName}.json")
        
        val mainObj: JsonObject
        val variants: JsonObject
        
        if (file.exists()) {
            mainObj = file.parseJson() as JsonObject
            variants = mainObj.getAsJsonObject("variants")
        } else {
            mainObj = JsonObject()
            variants = JsonObject()
            
            mainObj.add("variants", variants)
        }
        
        return Triple(file, mainObj, variants)
    }
    
}