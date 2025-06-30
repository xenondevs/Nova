@file:UseSerializers(Vector3dcAsArraySerializer::class, Vector4dcAsArraySerializer::class)

package xyz.xenondevs.nova.resources.builder.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector4d
import org.joml.Vector4dc
import org.joml.primitives.AABBd
import xyz.xenondevs.commons.collections.enumMapOf
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.task.ModelContent
import xyz.xenondevs.nova.serialization.kotlinx.Vector3dcAsArraySerializer
import xyz.xenondevs.nova.serialization.kotlinx.Vector4dcAsArraySerializer
import java.util.*

/**
 * A [Minecraft model](https://minecraft.wiki/w/Model).
 *
 * @param parent The path to the parent model or `null` if there is no parent.
 * @param textures A map of texture names to texture paths.
 * @param elements A list of voxels that make up the model.
 * @param ambientOcclusion (Only relevant for block models) Whether ambient occlusion is enabled for the model.
 * @param guiLight (Only relevant for item models) The direction of the light for the item model in the GUI.
 * @param display (Only relevant for item models) Context-dependant display settings.
 */
@Serializable
data class Model(
    val parent: ResourcePath<ResourceType.Model>? = null,
    val textures: Map<String, String> = emptyMap(),
    val elements: List<Element>? = null,
    
    // block-specific
    @SerialName("ambientocclusion")
    val ambientOcclusion: Boolean? = null,
    
    // item-specific
    @SerialName("gui_light")
    val guiLight: GuiLight? = null,
    val display: Display? = null
) {
    
    /**
     * Specifies the direction of the light for the item model in the GUI.
     */
    @Serializable
    enum class GuiLight {
        
        @SerialName("front")
        FRONT,
        
        @SerialName("side")
        SIDE
        
    }
    
    /**
     * An axis in 3D space.
     */
    @Serializable
    enum class Axis {
        
        @SerialName("x")
        X,
        
        @SerialName("y")
        Y,
        
        @SerialName("z")
        Z
        
    }
    
    /**
     * A direction in 3D space.
     */
    @Serializable
    enum class Direction(val axis: Axis) {
        
        @SerialName("north")
        NORTH(Axis.Z),
        
        @SerialName("east")
        EAST(Axis.X),
        
        @SerialName("south")
        SOUTH(Axis.Z),
        
        @SerialName("west")
        WEST(Axis.X),
        
        @SerialName("up")
        UP(Axis.Y),
        
        @SerialName("down")
        DOWN(Axis.Y)
        
    }
    
    /**
     * A voxel of a [Model].
     *
     * @param from The start position of the voxel.
     * @param to The end position of the voxel.
     * @param rotation The rotation of the voxel.
     * @param faces A map of the voxel's faces.
     * @params shade Whether shadows are rendered.
     */
    @Serializable
    data class Element(
        val from: Vector3dc,
        val to: Vector3dc,
        val rotation: Rotation? = null,
        val faces: Map<Direction, Face>,
        val shade: Boolean = true,
        @SerialName("light_emission")
        val lightEmission: Int = 0
    ) {
        
        /**
         * A face of an [Element].
         *
         * @param uv The area of the texture in the format (fromX, fromY, toX, toY).
         * @param texture The name of the texture. Resolved from the [Model.textures] map.
         * @param cullface Used to check whether the face does not need to be rendered if an occluding block is present
         * in the given direction.
         * @param rotation The rotation of the texture.
         * @param tintIndex Specifies the tint color for certain block- and item types.
         */
        @Serializable
        data class Face(
            val uv: Vector4dc? = null,
            val texture: String,
            val cullface: Direction? = null,
            val rotation: Int = 0,
            @SerialName("tintindex")
            val tintIndex: Int = -1
        )
        
        /**
         * The rotation of an [Element].
         *
         * @param angle The angle of the rotation. Can be 45.0, 22.5, -22.5, -45.0
         * @param axis The axis of the rotation.
         * @param origin The origin / pivot point of the rotation.
         * @param rescale Whether the model should be rescaled to fit the new size.
         * (for example a 45° rotation stretches the element by sqrt(2))
         */
        @Serializable
        data class Rotation(
            val angle: Double,
            val axis: Axis,
            val origin: Vector3dc,
            val rescale: Boolean = false
        )
        
        /**
         * Generates the UV coordinates for a face based the position of this [Element] for the given [direction].
         * This results in the same UV coordinates as those that are automatically generated by the client
         * for faces that do not specify UV coordinates.
         */
        fun generateUV(direction: Direction): Vector4dc =
            when (direction) {
                Direction.NORTH, Direction.SOUTH -> Vector4d(from.x(), from.y(), to.x(), to.y())
                Direction.EAST, Direction.WEST -> Vector4d(from.z(), from.y(), to.z(), to.y())
                Direction.UP, Direction.DOWN -> Vector4d(from.x(), from.z(), to.x(), to.z())
            }
        
    }
    
    /**
     * The display settings for an item model, depending on the context.
     * 
     * @param thirdPersonRightHand The display settings for right hand in third person.
     * @param thirdPersonLeftHand The display settings for left hand in third person. Defaults to [thirdPersonRightHand] if unspecified.
     * @param firstPersonRightHand The display settings for right hand in first person.
     * @param firstPersonLeftHand The display settings for left hand in first person. Defaults to [firstPersonRightHand] if unspecified.
     * @param head The display settings for the head.
     * @param gui The display settings for the GUI.
     * @param ground The display settings for the ground.
     * @param fixed The display settings for fixed position.
     */
    @Serializable
    data class Display(
        @SerialName("thirdperson_righthand")
        val thirdPersonRightHand: Entry = Entry(),
        @SerialName("thirdperson_lefthand")
        val thirdPersonLeftHand: Entry = thirdPersonRightHand,
        @SerialName("firstperson_righthand")
        val firstPersonRightHand: Entry = Entry(),
        @SerialName("firstperson_lefthand")
        val firstPersonLeftHand: Entry = firstPersonRightHand,
        val head: Entry = Entry(),
        val gui: Entry = Entry(),
        val ground: Entry = Entry(),
        val fixed: Entry = Entry()
    ) {
        
        companion object {
            
            /**
             * Creates a [Display] based on a map of [Position] to [Entry].
             */
            fun of(map: Map<Position, Entry>): Display {
                return Display(
                    thirdPersonRightHand = map[Position.THIRDPERSON_RIGHTHAND] ?: Entry(),
                    thirdPersonLeftHand = map[Position.THIRDPERSON_LEFTHAND] ?: map[Position.THIRDPERSON_RIGHTHAND] ?: Entry(),
                    firstPersonRightHand = map[Position.FIRSTPERSON_RIGHTHAND] ?: Entry(),
                    firstPersonLeftHand = map[Position.FIRSTPERSON_LEFTHAND] ?: map[Position.FIRSTPERSON_RIGHTHAND] ?: Entry(),
                    head = map[Position.HEAD] ?: Entry(),
                    gui = map[Position.GUI] ?: Entry(),
                    ground = map[Position.GROUND] ?: Entry(),
                    fixed = map[Position.FIXED] ?: Entry()
                )
            }
            
        }
        
        /**
         * Converts this [Display] to a map of [Position] to [Entry].
         */
        fun toMap(): Map<Position, Entry> = enumMapOf(
            Position.THIRDPERSON_RIGHTHAND to thirdPersonRightHand,
            Position.THIRDPERSON_LEFTHAND to thirdPersonLeftHand,
            Position.FIRSTPERSON_RIGHTHAND to firstPersonRightHand,
            Position.FIRSTPERSON_LEFTHAND to firstPersonLeftHand,
            Position.HEAD to head,
            Position.GUI to gui,
            Position.GROUND to ground,
            Position.FIXED to fixed
        )
        
        /**
         * The display settings for an item model.
         *
         * @param rotation The rotation of the model.
         * @param translation The translation of the model.
         * @param scale The scale of the model.
         */
        @Serializable
        data class Entry(
            val rotation: Vector3dc = Vector3d(0.0, 0.0, 0.0),
            val translation: Vector3dc = Vector3d(0.0, 0.0, 0.0),
            val scale: Vector3dc = Vector3d(1.0, 1.0, 1.0)
        )
        
        /**
         * The different contexts in a [Display].
         */
        @Serializable
        enum class Position {
            
            @SerialName("thirdperson_righthand")
            THIRDPERSON_RIGHTHAND,
            
            @SerialName("thirdperson_lefthand")
            THIRDPERSON_LEFTHAND,
            
            @SerialName("firstperson_righthand")
            FIRSTPERSON_RIGHTHAND,
            
            @SerialName("firstperson_lefthand")
            FIRSTPERSON_LEFTHAND,
            
            @SerialName("head")
            HEAD,
            
            @SerialName("gui")
            GUI,
            
            @SerialName("ground")
            GROUND,
            
            @SerialName("fixed")
            FIXED
            
        }
        
    }
    
    // TODO: verify whether this is the correct overwriting behavior for things like textures, display, guiLight, etc.
    /**
     * Creates a flattened copy of this model using the given [context] to resolve parent models.
     */
    fun flattened(context: ModelContent): Model {
        if (parent == null)
            return this
        
        val textures = HashMap<String, String>()
        var elements: List<Element>? = null
        var ambientOcclusion = true
        var guiLight = GuiLight.SIDE
        var display: Display? = null
        
        val hierarchy = LinkedList<Model>()
        var parent: Model? = this
        while (parent != null) {
            hierarchy.addFirst(parent)
            parent = parent.parent?.let(context::get)
        }
        
        while (hierarchy.isNotEmpty()) {
            val model = hierarchy.removeFirst()
            
            textures.putAll(model.textures)
            if (model.elements != null) elements = model.elements
            if (model.ambientOcclusion != null) ambientOcclusion = model.ambientOcclusion
            if (model.guiLight != null) guiLight = model.guiLight
            if (model.display != null) display = model.display
        }
        
        return Model(
            textures = textures,
            elements = elements,
            ambientOcclusion = ambientOcclusion,
            guiLight = guiLight,
            display = display
        )
    }
    
    /**
     * Gets the bounds of this model's elements. Uses [context] to resolve parent models.
     */
    fun getBounds(context: ModelContent?): AABBd {
        var elements: List<Element>? = null
        var parent: Model? = this
        while (elements == null && parent != null) {
            elements = parent.elements
            parent = parent.parent?.let { context?.get(it) }
        }
        
        if (elements == null)
            return AABBd()
        
        val min = Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
        val max = Vector3d(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE)
        for (element in elements) {
            min.min(element.from)
            max.max(element.to)
        }
        
        return AABBd(min, max)
    }
    
}