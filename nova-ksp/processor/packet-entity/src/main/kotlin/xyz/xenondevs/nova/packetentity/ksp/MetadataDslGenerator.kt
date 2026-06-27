package xyz.xenondevs.nova.packetentity.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

//<editor-fold desc="Type constants" defaultstate="collapsed">

private val PROVIDER = ClassName("xyz.xenondevs.commons.provider", "Provider")
private val DEFAULT_ENTITY_VALUE = ClassName("xyz.xenondevs.nova.packetentity", "DefaultEntityValue")
private val DSL_PROPERTY = ClassName("xyz.xenondevs.commons.provider.dsl", "DslProperty")
private val CONVERTING_DSL_PROPERTY = ClassName("xyz.xenondevs.nova.packetentity", "ConvertingDslProperty")
private val FLAGS_DSL_PROPERTY = ClassName("xyz.xenondevs.nova.packetentity", "FlagsDslProperty")
private val FLAGS_DSL_PROPERTY_IMPL = ClassName("xyz.xenondevs.nova.packetentity", "FlagsDslPropertyImpl")
private val FLAGS_STATE = ClassName("xyz.xenondevs.nova.packetentity", "FlagsState")
private val MUTABLE_FLAGS = ClassName("xyz.xenondevs.nova.packetentity", "MutableFlags")
private val MUTABLE_FLAGS_IMPL = ClassName("xyz.xenondevs.nova.packetentity", "MutableFlagsImpl")
private val PACKET_ENTITY_DSL_MARKER = ClassName("xyz.xenondevs.nova.packetentity", "PacketEntityDslMarker")
private val REACTIVE_DATA_VALUE = ClassName("xyz.xenondevs.nova.packetentity", "ReactiveDataValue")
private val DEFAULT_VALUES = ClassName("xyz.xenondevs.nova.packetentity", "DefaultValues")
private val ENTITY_DATA_SERIALIZERS = ClassName("net.minecraft.network.syncher", "EntityDataSerializers")
private val REGISTRIES = ClassName("net.minecraft.core.registries", "Registries")

private val OPTIONAL = ClassName("java.util", "Optional")
private val OPTIONAL_INT = ClassName("java.util", "OptionalInt")
private val HOLDER = ClassName("net.minecraft.core", "Holder")
private val NMS_COMPONENT = ClassName("net.minecraft.network.chat", "Component")
private val NMS_PARTICLE_OPTIONS = ClassName("net.minecraft.core.particles", "ParticleOptions")
private val ADVENTURE_COMPONENT = ClassName("net.kyori.adventure.text", "Component")

//</editor-fold>

//<editor-fold desc="Type mappings" defaultstate="collapsed">

private data class SerializerMapping(
    val nmsType: TypeName,
    val apiType: TypeName,
    val factoryName: String,
    val useHolderDefault: Boolean = false
)

private fun mapping(
    name: String,
    nmsType: TypeName,
    apiType: TypeName = nmsType,
    useHolderDefault: Boolean = false
): Pair<String, SerializerMapping> =
    name to SerializerMapping(nmsType, apiType, name, useHolderDefault)

private val SERIALIZER_MAPPINGS = mapOf(
    mapping("BYTE", ClassName("kotlin", "Byte")),
    mapping("INT", ClassName("kotlin", "Int")),
    mapping("FLOAT", ClassName("kotlin", "Float")),
    mapping("LONG", ClassName("kotlin", "Long")),
    mapping("BOOLEAN", ClassName("kotlin", "Boolean")),
    mapping("STRING", ClassName("kotlin", "String")),
    mapping("COMPONENT", NMS_COMPONENT, ADVENTURE_COMPONENT),
    mapping("OPTIONAL_COMPONENT", OPTIONAL.parameterizedBy(NMS_COMPONENT), ADVENTURE_COMPONENT.copy(nullable = true)),
    mapping("ITEM_STACK", ClassName("net.minecraft.world.item", "ItemStack"), ClassName("org.bukkit.inventory", "ItemStack")),
    mapping("BLOCK_STATE", ClassName("net.minecraft.world.level.block.state", "BlockState"), ClassName("org.bukkit.block.data", "BlockData")),
    mapping("OPTIONAL_BLOCK_STATE", OPTIONAL.parameterizedBy(ClassName("net.minecraft.world.level.block.state", "BlockState")), ClassName("org.bukkit.block.data", "BlockData").copy(nullable = true)),
    mapping("BLOCK_POS", ClassName("net.minecraft.core", "BlockPos"), ClassName("org.joml", "Vector3ic")),
    mapping("OPTIONAL_BLOCK_POS", OPTIONAL.parameterizedBy(ClassName("net.minecraft.core", "BlockPos")), ClassName("org.joml", "Vector3ic").copy(nullable = true)),
    mapping("DIRECTION", ClassName("net.minecraft.core", "Direction"), ClassName("org.bukkit.block", "BlockFace")),
    mapping("ROTATIONS", ClassName("net.minecraft.core", "Rotations"), ClassName("org.joml", "Vector3fc")),
    mapping("POSE", ClassName("net.minecraft.world.entity", "Pose"), ClassName("org.bukkit.entity", "Pose")),
    mapping("VECTOR3", ClassName("org.joml", "Vector3fc")),
    mapping("QUATERNION", ClassName("org.joml", "Quaternionfc")),
    mapping("OPTIONAL_UNSIGNED_INT", OPTIONAL_INT, ClassName("kotlin", "Int").copy(nullable = true)),
    mapping(
        "OPTIONAL_LIVING_ENTITY_REFERENCE",
        OPTIONAL.parameterizedBy(ClassName("net.minecraft.world.entity", "EntityReference").parameterizedBy(ClassName("net.minecraft.world.entity", "LivingEntity"))),
        ClassName("java.util", "UUID").copy(nullable = true)
    ),
    mapping("OPTIONAL_GLOBAL_POS", OPTIONAL.parameterizedBy(ClassName("net.minecraft.core", "GlobalPos")), ClassName("org.bukkit", "Location").copy(nullable = true)),
    mapping("HUMANOID_ARM", ClassName("net.minecraft.world.entity", "HumanoidArm"), ClassName("org.bukkit.inventory", "MainHand")),
    mapping("RESOLVABLE_PROFILE", ClassName("net.minecraft.world.item.component", "ResolvableProfile"), ClassName("io.papermc.paper.datacomponent.item", "ResolvableProfile")),
    mapping("PAINTING_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.decoration.painting", "PaintingVariant")), ClassName("org.bukkit", "Art"), useHolderDefault = true),
    mapping("PARTICLE", NMS_PARTICLE_OPTIONS),
    mapping("PARTICLES", LIST.parameterizedBy(NMS_PARTICLE_OPTIONS)),
    mapping("VILLAGER_DATA", ClassName("net.minecraft.world.entity.npc.villager", "VillagerData")),
    mapping("ARMADILLO_STATE", ClassName("net.minecraft.world.entity.animal.armadillo", "Armadillo", "ArmadilloState"), ClassName("org.bukkit.entity", "Armadillo", "State")),
    mapping("SNIFFER_STATE", ClassName("net.minecraft.world.entity.animal.sniffer", "Sniffer", "State"), ClassName("org.bukkit.entity", "Sniffer", "State")),
    mapping("WEATHERING_COPPER_STATE", ClassName("net.minecraft.world.level.block", "WeatheringCopper", "WeatherState"), ClassName("io.papermc.paper.world", "WeatheringCopperState")),
    mapping("COPPER_GOLEM_STATE", ClassName("net.minecraft.world.entity.animal.golem", "CopperGolemState"), ClassName("org.bukkit.entity", "CopperGolem", "State")),
    mapping("CAT_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.feline", "CatVariant")), ClassName("org.bukkit.entity", "Cat", "Type"), useHolderDefault = true),
    mapping("CAT_SOUND_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.feline", "CatSoundVariant")), ClassName("org.bukkit.entity", "Cat", "SoundVariant"), useHolderDefault = true),
    mapping("WOLF_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.wolf", "WolfVariant")), ClassName("org.bukkit.entity", "Wolf", "Variant"), useHolderDefault = true),
    mapping("WOLF_SOUND_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.wolf", "WolfSoundVariant")), ClassName("org.bukkit.entity", "Wolf", "SoundVariant"), useHolderDefault = true),
    mapping("FROG_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.frog", "FrogVariant")), ClassName("org.bukkit.entity", "Frog", "Variant"), useHolderDefault = true),
    mapping("PIG_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.pig", "PigVariant")), ClassName("org.bukkit.entity", "Pig", "Variant"), useHolderDefault = true),
    mapping("PIG_SOUND_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.pig", "PigSoundVariant")), ClassName("org.bukkit.entity", "Pig", "SoundVariant"), useHolderDefault = true),
    mapping("CHICKEN_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.chicken", "ChickenVariant")), ClassName("org.bukkit.entity", "Chicken", "Variant"), useHolderDefault = true),
    mapping("CHICKEN_SOUND_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.chicken", "ChickenSoundVariant")), ClassName("org.bukkit.entity", "Chicken", "SoundVariant"), useHolderDefault = true),
    mapping("COW_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.cow", "CowVariant")), ClassName("org.bukkit.entity", "Cow", "Variant"), useHolderDefault = true),
    mapping("COW_SOUND_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.cow", "CowSoundVariant")), ClassName("org.bukkit.entity", "Cow", "SoundVariant"), useHolderDefault = true),
    mapping("ZOMBIE_NAUTILUS_VARIANT", HOLDER.parameterizedBy(ClassName("net.minecraft.world.entity.animal.nautilus", "ZombieNautilusVariant")), ClassName("org.bukkit.entity", "ZombieNautilus", "Variant"), useHolderDefault = true)
)

private fun serializerMapping(serializerType: String): SerializerMapping =
    SERIALIZER_MAPPINGS[serializerType] ?: SerializerMapping(ClassName("kotlin", "Any"), ClassName("kotlin", "Any"), serializerType)

//</editor-fold>

//<editor-fold desc="Name utilities" defaultstate="collapsed">

private fun toClassName(internalClassName: String): String {
    val simpleName = internalClassName.substringAfterLast('/').substringAfterLast('$')
    return "${simpleName}MetadataDsl"
}

private fun toMetadataClassName(internalClassName: String): String {
    val simpleName = internalClassName.substringAfterLast('/').substringAfterLast('$')
    return "${simpleName}Metadata"
}

private fun toStateClassName(internalClassName: String): String {
    val simpleName = internalClassName.substringAfterLast('/').substringAfterLast('$')
    return "${simpleName}MetadataState"
}

private fun toPropertyName(fieldName: String): String {
    var name = fieldName
    name = name.removePrefix("DATA_")
    name = name.removePrefix("ID_")
    name = name.removeSuffix("_ID")
    return name.split("_")
        .mapIndexed { i, part ->
            if (i == 0) part.lowercase()
            else part.lowercase().replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
}

private fun findNearestAncestorWithFields(
    className: String,
    entityData: Map<String, EntityClassData>,
    superclasses: Map<String, String>
): String? {
    var current = superclasses[className]
    while (current != null && current != "java/lang/Object") {
        if (current in entityData) return current
        current = superclasses[current]
    }
    return null
}

private fun internalNameToClassName(internalName: String): ClassName {
    val lastSlash = internalName.lastIndexOf('/')
    val packageName = if (lastSlash >= 0) internalName.substring(0, lastSlash).replace('/', '.') else ""
    val classNames = internalName.substring(lastSlash + 1).split('$')
    return ClassName(packageName, classNames.first(), *classNames.drop(1).toTypedArray())
}

private fun isFlagsField(field: EntityDataField): Boolean =
    field.serializerType == "BYTE" && "FLAG" in field.fieldName

//</editor-fold>

//<editor-fold desc="Default value codegen" defaultstate="collapsed">

private fun camelToScreamingSnake(name: String): String =
    name.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

private fun defaultValueConstantName(className: String, fieldName: String): String {
    val simpleName = className.substringAfterLast('/').substringAfterLast('$')
    val propName = toPropertyName(fieldName)
    return "${camelToScreamingSnake(simpleName)}_${camelToScreamingSnake(propName)}"
}

private fun coerceDefault(value: DefaultValue, serializerType: String): DefaultValue {
    if (value !is DefaultValue.IntConst) return value
    return when (serializerType) {
        "BOOLEAN" -> DefaultValue.BoolConst(value.value != 0)
        "BYTE" -> DefaultValue.ByteConst(value.value)
        else -> value
    }
}

private fun defaultValueToCodeBlock(value: DefaultValue, serializerType: String, className: String, fieldName: String): CodeBlock = when (value) {
    is DefaultValue.IntConst -> CodeBlock.of("%L", value.value)
    is DefaultValue.LongConst -> CodeBlock.of("%LL", value.value)
    is DefaultValue.FloatConst -> CodeBlock.of("%Lf", value.value)
    is DefaultValue.DoubleConst -> CodeBlock.of("%L", value.value)
    is DefaultValue.StringConst -> CodeBlock.of("%S", value.value)
    is DefaultValue.BoolConst -> CodeBlock.of("%L", value.value)
    is DefaultValue.ByteConst -> CodeBlock.of("(%L).toByte()", value.value)
    is DefaultValue.NullValue -> CodeBlock.of("null")
    is DefaultValue.StaticField -> CodeBlock.of("%T.%L", internalNameToClassName(value.owner), value.name)
    is DefaultValue.StaticCall -> CodeBlock.builder().apply {
        add("%T.%L(", internalNameToClassName(value.owner), value.name)
        value.args.forEachIndexed { i, arg ->
            if (i > 0) add(", ")
            add(defaultValueToCodeBlock(arg, serializerType, className, fieldName))
        }
        add(")")
    }.build()
    is DefaultValue.ConstructorCall -> CodeBlock.builder().apply {
        add("%T(", internalNameToClassName(value.type))
        value.args.forEachIndexed { i, arg ->
            if (i > 0) add(", ")
            add(defaultValueToCodeBlock(arg, serializerType, className, fieldName))
        }
        add(")")
    }.build()
    is DefaultValue.InstanceCall -> CodeBlock.builder().apply {
        add(defaultValueToCodeBlock(value.receiver, serializerType, className, fieldName))
        add(".%L(", value.method)
        value.args.forEachIndexed { i, arg ->
            if (i > 0) add(", ")
            add(defaultValueToCodeBlock(arg, serializerType, className, fieldName))
        }
        add(")")
    }.build()
    is DefaultValue.Unknown -> {
        if (serializerMapping(serializerType).useHolderDefault) {
            CodeBlock.of("%T.holderDefault(%T.%L)", DEFAULT_VALUES, REGISTRIES, serializerType)
        } else {
            CodeBlock.of("%T.%L", DEFAULT_VALUES, defaultValueConstantName(className, fieldName))
        }
    }
}

//</editor-fold>

private const val GENERATED_PACKAGE = "xyz.xenondevs.nova.packetentity"
private const val PROPERTY_OVERRIDE_ANNOTATION = "$GENERATED_PACKAGE.EntityDataPropertyOverride"

private data class PropertyOverride(
    val factoryName: String,
    val apiType: TypeName
)

internal class MetadataDslGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    fun generate(analysisResult: AnalysisResult, resolver: Resolver) {
        val entityData = analysisResult.entityData
        val superclasses = analysisResult.superclasses
        val overrideSymbols = resolver.getSymbolsWithAnnotation(PROPERTY_OVERRIDE_ANNOTATION).toList()
        val overrides = findPropertyOverrides(overrideSymbols)
        val matchedOverrides = HashSet<Pair<String, String>>()

        val fileSpec = FileSpec.builder(GENERATED_PACKAGE, "EntityMetadataDsl")
            .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())

        for ([_, classData] in entityData) {
            val parentName = findNearestAncestorWithFields(classData.className, entityData, superclasses)
            fileSpec.addType(buildInterface(classData, parentName, overrides, matchedOverrides))
            fileSpec.addType(buildMutableInterface(classData, parentName, overrides, matchedOverrides))
            fileSpec.addType(buildState(classData, parentName))
            fileSpec.addType(buildImpl(classData, parentName, overrides, matchedOverrides))
            fileSpec.addType(buildMutableImpl(classData, parentName, overrides, matchedOverrides))
        }

        val unmatchedOverrides = overrides.keys - matchedOverrides
        for ([owner, property] in unmatchedOverrides) {
            logger.error("Entity data property override does not match a generated property: $owner.$property")
        }

        val sourceFiles = overrideSymbols.mapNotNullTo(LinkedHashSet<KSFile>()) {
            (it as? KSDeclaration)?.containingFile
        }
        fileSpec.build().writeTo(codeGenerator, Dependencies(true, *sourceFiles.toTypedArray()))
    }

    private fun findPropertyOverrides(symbols: List<KSAnnotated>): Map<Pair<String, String>, PropertyOverride> {
        val overrides = HashMap<Pair<String, String>, PropertyOverride>()
        for (symbol in symbols) {
            val property = symbol as? KSPropertyDeclaration ?: continue
            val apiType = property.type.resolve().arguments.getOrNull(1)?.type?.toTypeName()
            if (apiType == null) {
                logger.error("@EntityDataPropertyOverride can only be used on ConvertingDslPropertyFactory<B, N> properties", property)
                continue
            }
            
            for (annotation in property.annotations) {
                if (annotation.annotationType.resolve().declaration.qualifiedName?.asString() != PROPERTY_OVERRIDE_ANNOTATION)
                    continue
                
                val args = annotation.arguments.associate { it.name?.asString() to it.value }
                val owner = args["owner"] as? String
                val prop = args["property"] as? String
                if (owner == null || prop == null) {
                    logger.error("@EntityDataPropertyOverride requires owner and property", property)
                    continue
                }
                
                val key = owner to prop
                val previous = overrides.put(key, PropertyOverride(property.simpleName.asString(), apiType))
                if (previous != null) {
                    logger.error("Duplicate entity data property override for $owner.$prop", property)
                }
            }
        }
        return overrides
    }

    private fun classOverrideOwnerName(className: String): String =
        className.substringAfterLast('/').substringAfterLast('$')

    private fun propertyOverride(
        classData: EntityClassData,
        propName: String,
        overrides: Map<Pair<String, String>, PropertyOverride>,
        matchedOverrides: MutableSet<Pair<String, String>>
    ): PropertyOverride? {
        val key = classOverrideOwnerName(classData.className) to propName
        val override = overrides[key] ?: return null
        matchedOverrides += key
        return override
    }

    private fun buildInterface(
        classData: EntityClassData,
        parentName: String?,
        overrides: Map<Pair<String, String>, PropertyOverride>,
        matchedOverrides: MutableSet<Pair<String, String>>
    ): TypeSpec {
        val interfaceName = toClassName(classData.className)
        val builder = TypeSpec.interfaceBuilder(interfaceName)
            .addModifiers(KModifier.SEALED)

        if (parentName != null) {
            builder.addSuperinterface(ClassName(GENERATED_PACKAGE, toClassName(parentName)))
        } else {
            builder.addAnnotation(PACKET_ENTITY_DSL_MARKER)
        }

        for (field in classData.fields) {
            val propName = toPropertyName(field.fieldName)
            if (isFlagsField(field)) {
                builder.addProperty(
                    PropertySpec.builder(propName, FLAGS_DSL_PROPERTY).build()
                )
            } else {
                val propertyType = propertyOverride(classData, propName, overrides, matchedOverrides)?.apiType
                    ?: serializerMapping(field.serializerType).apiType
                builder.addProperty(
                    PropertySpec.builder(propName, DSL_PROPERTY.parameterizedBy(propertyType)).build()
                )
            }
        }

        return builder.build()
    }

    private fun buildMutableInterface(
        classData: EntityClassData,
        parentName: String?,
        overrides: Map<Pair<String, String>, PropertyOverride>,
        matchedOverrides: MutableSet<Pair<String, String>>
    ): TypeSpec {
        val interfaceName = toMetadataClassName(classData.className)
        val builder = TypeSpec.interfaceBuilder(interfaceName)
            .addModifiers(KModifier.SEALED)

        if (parentName != null) {
            builder.addSuperinterface(ClassName(GENERATED_PACKAGE, toMetadataClassName(parentName)))
        } else {
            builder.addAnnotation(PACKET_ENTITY_DSL_MARKER)
        }

        for (field in classData.fields) {
            val propName = toPropertyName(field.fieldName)
            if (isFlagsField(field)) {
                builder.addProperty(
                    PropertySpec.builder(propName, MUTABLE_FLAGS).build()
                )
            } else {
                val propertyType = propertyOverride(classData, propName, overrides, matchedOverrides)?.apiType
                    ?: serializerMapping(field.serializerType).apiType
                builder.addProperty(
                    PropertySpec.builder(propName, propertyType).mutable(true).build()
                )
            }
        }

        return builder.build()
    }

    private fun buildImpl(
        classData: EntityClassData,
        parentName: String?,
        overrides: Map<Pair<String, String>, PropertyOverride>,
        matchedOverrides: MutableSet<Pair<String, String>>
    ): TypeSpec {
        val interfaceName = toClassName(classData.className)
        val implName = "${interfaceName}Impl"
        val stateName = toStateClassName(classData.className)
        val stateParam = "metadataState"
        val builder = TypeSpec.classBuilder(implName)
            .addModifiers(KModifier.INTERNAL, KModifier.OPEN)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(stateParam, ClassName(GENERATED_PACKAGE, stateName))
                    .build()
            )
            .addSuperinterface(ClassName(GENERATED_PACKAGE, interfaceName))
            .addProperty(
                PropertySpec.builder(stateParam, ClassName(GENERATED_PACKAGE, stateName))
                    .addModifiers(buildList {
                        add(KModifier.PROTECTED)
                        add(KModifier.OPEN)
                        if (parentName != null) add(KModifier.OVERRIDE)
                    })
                    .initializer(stateParam)
                    .build()
            )

        if (parentName != null) {
            builder.superclass(ClassName(GENERATED_PACKAGE, "${toClassName(parentName)}Impl"))
            builder.addSuperclassConstructorParameter(stateParam)
        }

        for (field in classData.fields) {
            val propName = toPropertyName(field.fieldName)

            if (isFlagsField(field)) {
                builder.addProperty(
                    PropertySpec.builder(propName, FLAGS_DSL_PROPERTY)
                        .addModifiers(KModifier.OVERRIDE)
                        .getter(FunSpec.getterBuilder().addStatement("return %T(%L.%L)", FLAGS_DSL_PROPERTY_IMPL, stateParam, propName).build())
                        .build()
                )
            } else {
                val override = propertyOverride(classData, propName, overrides, matchedOverrides)
                builder.addProperty(buildDslProperty(propName, "$stateParam.$propName", field.serializerType, override))
            }
        }

        return builder.build()
    }

    private fun buildMutableImpl(
        classData: EntityClassData,
        parentName: String?,
        overrides: Map<Pair<String, String>, PropertyOverride>,
        matchedOverrides: MutableSet<Pair<String, String>>
    ): TypeSpec {
        val implName = "${toMetadataClassName(classData.className)}Impl"
        val mutableInterfaceName = toMetadataClassName(classData.className)
        val stateName = toStateClassName(classData.className)
        val stateParam = "metadataState"
        val builder = TypeSpec.classBuilder(implName)
            .addModifiers(KModifier.INTERNAL, KModifier.OPEN)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(stateParam, ClassName(GENERATED_PACKAGE, stateName))
                    .build()
            )
            .addSuperinterface(ClassName(GENERATED_PACKAGE, mutableInterfaceName))
            .addProperty(
                PropertySpec.builder(stateParam, ClassName(GENERATED_PACKAGE, stateName))
                    .addModifiers(buildList {
                        add(KModifier.PROTECTED)
                        add(KModifier.OPEN)
                        if (parentName != null) add(KModifier.OVERRIDE)
                    })
                    .initializer(stateParam)
                    .build()
            )

        if (parentName != null) {
            builder.superclass(ClassName(GENERATED_PACKAGE, "${toMetadataClassName(parentName)}Impl"))
            builder.addSuperclassConstructorParameter(stateParam)
        }

        for (field in classData.fields) {
            val propName = toPropertyName(field.fieldName)
            if (isFlagsField(field)) {
                builder.addProperty(
                    PropertySpec.builder(propName, MUTABLE_FLAGS)
                        .addModifiers(KModifier.OVERRIDE)
                        .getter(FunSpec.getterBuilder().addStatement("return %T(%L.%L)", MUTABLE_FLAGS_IMPL, stateParam, propName).build())
                        .build()
                )
            } else {
                val override = propertyOverride(classData, propName, overrides, matchedOverrides)
                builder.addProperty(buildMutableProperty(propName, "$stateParam.$propName", field.serializerType, override))
            }
        }

        return builder.build()
    }

    private fun buildState(classData: EntityClassData, parentName: String?): TypeSpec {
        val builder = TypeSpec.classBuilder(toStateClassName(classData.className))
            .addModifiers(KModifier.INTERNAL, KModifier.OPEN)

        if (parentName != null) {
            builder.superclass(ClassName(GENERATED_PACKAGE, toStateClassName(parentName)))
        }

        for (field in classData.fields) {
            val propName = toPropertyName(field.fieldName)
            if (isFlagsField(field)) {
                builder.addProperty(buildSharedFlagProperty(propName))
            } else {
                val coercedDefault = coerceDefault(field.defaultValue, field.serializerType)
                builder.addProperty(buildBackingProperty(propName, serializerMapping(field.serializerType).nmsType, defaultValueToCodeBlock(coercedDefault, field.serializerType, classData.className, field.fieldName)))
            }
        }

        builder.addFunction(buildComponentsFunction(classData, parentName != null))
        return builder.build()
    }

    private fun buildBackingProperty(name: String, type: TypeName, defaultCode: CodeBlock): PropertySpec =
        PropertySpec.builder(name, DEFAULT_ENTITY_VALUE.parameterizedBy(type))
            .addModifiers(KModifier.INTERNAL)
            .initializer(CodeBlock.builder().add("%T(", DEFAULT_ENTITY_VALUE).add(defaultCode).add(")").build())
            .build()

    private fun buildDslProperty(name: String, backingName: String, serializerType: String, override: PropertyOverride?): PropertySpec {
        val mapping = serializerMapping(serializerType)
        val propertyType = override?.apiType ?: mapping.apiType
        val factoryName = override?.factoryName ?: mapping.factoryName
        return PropertySpec.builder(name, DSL_PROPERTY.parameterizedBy(propertyType))
            .addModifiers(KModifier.OVERRIDE)
            .getter(FunSpec.getterBuilder().addStatement("return %T.%L.create(%L)", CONVERTING_DSL_PROPERTY, factoryName, backingName).build())
            .build()
    }

    private fun buildMutableProperty(name: String, backingName: String, serializerType: String, override: PropertyOverride?): PropertySpec {
        val mapping = serializerMapping(serializerType)
        val propertyType = override?.apiType ?: mapping.apiType
        val factoryName = override?.factoryName ?: mapping.factoryName
        return PropertySpec.builder(name, propertyType)
            .mutable(true)
            .addModifiers(KModifier.OVERRIDE)
            .getter(FunSpec.getterBuilder().addStatement("return %T.%L.get(%L)", CONVERTING_DSL_PROPERTY, factoryName, backingName).build())
            .setter(FunSpec.setterBuilder().addParameter("value", propertyType).addStatement("%T.%L.set(%L, value)", CONVERTING_DSL_PROPERTY, factoryName, backingName).build())
            .build()
    }

    private fun buildSharedFlagProperty(name: String): PropertySpec =
        PropertySpec.builder(name, FLAGS_STATE)
            .initializer("%T()", FLAGS_STATE)
            .build()

    private fun buildComponentsFunction(classData: EntityClassData, hasParent: Boolean): FunSpec {
        val componentsType = ClassName("kotlin.collections", "MutableList")
            .parameterizedBy(REACTIVE_DATA_VALUE.parameterizedBy(STAR))
        val builder = FunSpec.builder("addComponents")
            .addModifiers(KModifier.INTERNAL)
            .addParameter("components", componentsType)

        if (hasParent) {
            builder.addModifiers(KModifier.OVERRIDE)
            builder.addStatement("super.addComponents(components)")
        } else {
            builder.addModifiers(KModifier.OPEN)
        }

        for (field in classData.fields) {
            if (isFlagsField(field)) {
                builder.addStatement(
                    "components.add(%T(%L.packedValue, %T.%L))",
                    REACTIVE_DATA_VALUE, toPropertyName(field.fieldName), ENTITY_DATA_SERIALIZERS, field.serializerType
                )
            } else {
                builder.addStatement(
                    "components.add(%T(%L, %T.%L))",
                    REACTIVE_DATA_VALUE, toPropertyName(field.fieldName), ENTITY_DATA_SERIALIZERS, field.serializerType
                )
            }
        }

        return builder.build()
    }
}
