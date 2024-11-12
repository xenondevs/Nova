package xyz.xenondevs.nova.resources.layout.gui

import org.joml.Vector2i
import org.joml.Vector2ic
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath

internal class GuiTextureLayout(
    val texture: ResourcePath,
    val alignment: GuiTextureAlignment
)

@RegistryElementBuilderDsl
class GuiTextureLayoutBuilder(private val namespace: String) {
    
    private var texture: ResourcePath? = null
    private var alignment: GuiTextureAlignment? = null
    
    /**
     * Sets the [path] to the gui texture.
     */
    fun path(path: ResourcePath) {
        this.texture = path
    }
    
    /**
     * Sets the path to the gui texture.
     */
    fun path(name: String) {
        this.texture = ResourcePath.of(name, namespace)
    }
    
    /**
     * Configures how the gui texture should be aligned.
     */
    fun alignment(alignment: GuiTextureAlignment) {
        this.alignment = alignment
    }
    
    /**
     * Configures a custom alignment with the given [offsetX] and [offsetY].
     */
    fun alignment(offsetX: Int, offsetY: Int) {
        alignment = GuiTextureAlignment.Custom(Vector2i(offsetX, offsetY))
    }
    
    /**
     * Configures a dynamic alignment based on the [alignment] function, which receives (width, height)
     * of the gui texture and returns the (x, y) offset.
     */
    fun alignment(alignment: (Int, Int) -> Vector2ic) {
        this.alignment = object : GuiTextureAlignment {
            override fun getOffset(width: Int, height: Int): Vector2ic {
                return alignment(width, height)
            }
        }
    }
    
    internal fun build(): GuiTextureLayout =
        GuiTextureLayout(
            texture ?: throw IllegalStateException("Gui texture path not set"),
            alignment ?: GuiTextureAlignment.ChestDefault
        )
    
}