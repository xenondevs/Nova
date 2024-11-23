@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.resources

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import org.bukkit.plugin.Plugin
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.serialization.kotlinx.ResourcePathSerializer

/**
 * An identifier for a file in a resource pack.
 *
 * Similar to [Key], [ResourcePath] consists of a [namespace] and [path], but with an additional [type] that specifies the
 * type of resource and thereby its location in the resource pack.
 */
@Serializable(with = ResourcePathSerializer::class)
class ResourcePath<out T : ResourceType>(val type: T, val namespace: String, val path: String) : Key {
    
    private val id = "$namespace:$path"
    
    /**
     * The full path to the file in the resource pack, starting with assets/.
     */
    val filePath = buildString {
        append("assets/$namespace/${type.prefix}/$path")
        if (type.suffix.isNotBlank())
            append(type.suffix)
    }
    
    init {
        require(isValidNamespace(namespace)) { "Non [a-z0-9_.-] character in $namespace" }
        require(isValidPath(path)) { "Non [a-z0-9_.-/] character in $path" }
    }
    
    /**
     * Changes the [type] of this [ResourcePath] to [newType], updating [path]
     * to still target the same file from the new type.
     *
     * @throws IllegalArgumentException If it is not possible to target the same file from the new type.
     */
    fun <NT : ResourceType> toType(newType: NT): ResourcePath<NT> {
        @Suppress("UNCHECKED_CAST")
        if (this.type == newType)
            return this as ResourcePath<NT>
        
        var fullPath = type.prefix + path
        if (type.suffix.isNotBlank())
            fullPath += type.suffix
        
        if (!fullPath.startsWith(newType.prefix))
            throw IllegalArgumentException("Cannot switch type to $newType, because $fullPath does not start with ${newType.prefix}")
        
        if (!fullPath.endsWith(newType.suffix))
            throw IllegalArgumentException("Cannot switch type to $newType, because $fullPath does not end with ${newType.suffix}")
        
        return ResourcePath(newType, namespace, fullPath.removePrefix(newType.prefix).removeSuffix(newType.suffix))
    }
    
    override fun namespace(): String = namespace
    override fun value(): String = path
    override fun asString(): String = id
    override fun toString(): String = id
    
    operator fun component1() = namespace
    operator fun component2() = path
    
    override fun hashCode(): Int = filePath.hashCode()
    override fun equals(other: Any?): Boolean = other is ResourcePath<*> && filePath == other.filePath
    
    companion object {
        
        /**
         * Creates a new [ResourcePath] in [type] using the given [key].
         */
        fun <T : ResourceType> of(type: T, key: Key): ResourcePath<T> =
            ResourcePath(type, key.namespace(), key.value())
        
        /**
         * Creates a new [ResourcePath] in [type] using the given [id], falling back to [fallbackNamespace]
         * if no namespace is specified in [id].
         */
        fun <T : ResourceType> of(type: T, id: String, fallbackNamespace: Addon): ResourcePath<T> =
            of(type, id, fallbackNamespace.id)
        
        /**
         * Creates a new [ResourcePath] in [type] using the given [id], falling back to [fallbackNamespace]
         * if no namespace is specified in [id].
         */
        fun <T : ResourceType> of(type: T, id: String, fallbackNamespace: Plugin): ResourcePath<T> =
            of(type, id, fallbackNamespace.name.lowercase())
        
        /**
         * Creates a new [ResourcePath] in [type] using the given [id], falling back to [fallbackNamespace]
         * if no namespace is specified in [id].
         */
        fun <T : ResourceType> of(type: T, id: String, fallbackNamespace: String = "minecraft"): ResourcePath<T> {
            val indexOfSeparator = id.indexOf(':')
            if (indexOfSeparator >= 0) {
                return ResourcePath(type, id.substring(0, indexOfSeparator), id.substring(indexOfSeparator + 1))
            } else {
                return ResourcePath(type, fallbackNamespace, id)
            }
        }
        
        /**
         * Checks whether the given [id] is a valid namespaced or non-namespaced resource path.
         */
        fun isValid(id: String): Boolean {
            val indexOfSeparator = id.indexOf(':')
            if (indexOfSeparator >= 0) {
                for (i in 0..<indexOfSeparator) {
                    if (!isValidNamespaceChar(id[i]))
                        return false
                }
                for (i in (indexOfSeparator + 1)..<id.length) {
                    if (!isValidPathChar(id[i]))
                        return false
                }
                return true
            } else {
                return isValidPath(id)
            }
        }
        
        /**
         * Checks whether the given [namespace] is a valid namespace.
         */
        fun isValidNamespace(namespace: String): Boolean =
            namespace.all(::isValidNamespaceChar)
        
        /**
         * Checks whether the given [path] is a valid path.
         */
        fun isValidPath(path: String): Boolean =
            path.all(::isValidPathChar)
        
        private fun isValidNamespaceChar(char: Char): Boolean =
            char in 'a'..'z' || char in '0'..'9' || char == '.' || char == '_' || char == '-'
        
        private fun isValidPathChar(char: Char): Boolean =
            isValidNamespaceChar(char) || char == '/'
        
    }
    
}

/**
 * Specifies the type of resource that a [ResourcePath] points to.
 */
sealed interface ResourceType {
    
    /**
     * The directory under which resources of this type are stored.
     */
    val prefix: String
    
    /**
     * The file extension of resources of this type.
     */
    val suffix: String
    
    /**
     * Marker interface that specifies that a [ResourceType] has a corresponding `.mcmeta` file.
     */
    sealed interface HasMcMeta : ResourceType
    
    /**
     * Resources of type `.json`.
     */
    sealed interface JsonFile : ResourceType {
        override val suffix
            get() = ".json"
    }
    
    /**
     * Resources of type `.png`.
     */
    sealed interface PngFile : ResourceType {
        override val suffix
            get() = ".png"
    }
    
    /**
     * Generic Item- and block models
     *
     * path: `models/`, extension: `json`
     */
    @Serializable
    data object Model : JsonFile {
        override val prefix = "models"
    }
    
    /**
     * Equipment definitions
     *
     * path: `models/equipment`, extension: `json`
     */
    @Serializable
    data object Equipment : JsonFile {
        override val prefix = "models/equipment"
    }
    
    /**
     * Resources that represent texture files.
     */
    interface Texture : PngFile {
        
        /**
         * Textures
         *
         * path: `textures/`, extension: `png`
         */
        @Serializable
        companion object : Texture {
            override val prefix = "textures"
        }
        
    }
    
    /**
     * Equipment textures
     */
    interface EquipmentTexture : Texture
    
    /**
     * Humanoid equipment textures
     *
     * path: `textures/entity/equipment/humanoid/`, extension: `png`
     */
    @Serializable
    data object HumanoidEquipmentTexture : EquipmentTexture {
        override val prefix = "textures/entity/equipment/humanoid"
    }
    
    /**
     * Humanoid leggings equipment textures
     *
     * path: `textures/entity/equipment/humanoid_leggings/`, extension: `png`
     */
    @Serializable
    data object HumanoidLegginsEquipmentTexture : EquipmentTexture {
        override val prefix = "textures/entity/equipment/humanoid_leggings"
    }
    
    /**
     * Wings equipment textures
     *
     * path: `textures/entity/equipment/wings/`, extension: `png`
     */
    @Serializable
    data object WingsEquipmentTexture : EquipmentTexture {
        override val prefix = "textures/entity/equipment/wings"
    }
    
    /**
     * Horse body equipment textures
     *
     * path: `textures/entity/equipment/horse_body/`, extension: `png`
     */
    @Serializable
    data object HorseBodyEquipmentTexture : EquipmentTexture {
        override val prefix = "textures/entity/equipment/horse_body"
    }
    
    
    /**
     * Llama body equipment textures
     *
     * path: `textures/entity/equipment/llama_body/`, extension: `png`
     */
    @Serializable
    data object LlamaBodyEquipmentTexture : EquipmentTexture {
        override val prefix = "textures/entity/equipment/llama_body"
    }
    
    /**
     * Wolf body equipment textures
     *
     * path: `textures/entity/equipment/wolf_body/`, extension: `png`
     */
    @Serializable
    data object WolfBodyEquipmentTexture : EquipmentTexture {
        override val prefix = "textures/entity/equipment/wolf_body"
    }
    
    /**
     * Tooltip textures
     * 
     * path: `textures/gui/sprites/tooltip/`, extension: `png`
     */
    interface TooltipTexture : Texture, HasMcMeta {
        
        override val prefix
            get() = "textures/gui/sprites/tooltip/"
        
        @Serializable
        companion object : TooltipTexture
    
    }
    
    /**
     * Tooltip background textures
     * 
     * path: `textures/gui/sprites/tooltip/`, suffix: `_background`, extension: `png`
     */
    @Serializable
    data object TooltipBackgroundTexture : TooltipTexture {
        override val suffix = "_background.png"
    }
    
    /**
     * Tooltip frame textures
     * 
     * path: `textures/gui/sprites/tooltip/`, suffix: `_frame`, extension: `png`
     */
    @Serializable
    data object TooltipFrameTexture : TooltipTexture {
        override val suffix = "_frame.png"
    }
    
    /**
     * Font definitions
     *
     * path: `font/`, extension: `json`
     */
    @Serializable
    data object Font : JsonFile {
        override val prefix = "font"
    }
    
    /**
     * Font textures. Assumed to contain the file extension `.png` inside [ResourcePath.path].
     *
     * path: `textures/`, extension: none
     */
    @Serializable
    data object FontTexture : Texture {
        override val prefix = "textures"
        override val suffix = ""
    }
    
    /**
     * Unihex zip files. Assumed to contain the file extension `.zip` inside [ResourcePath.path].
     * 
     * path: none, extension: none
     */
    @Serializable
    data object UnihexZip : ResourceType {
        override val prefix = ""
        override val suffix = ""
    }
    
}