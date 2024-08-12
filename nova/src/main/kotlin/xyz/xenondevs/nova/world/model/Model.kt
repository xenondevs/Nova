package xyz.xenondevs.nova.world.model

import org.bukkit.Location
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.Display.Brightness
import org.bukkit.inventory.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemDisplayMetadata
import xyz.xenondevs.nova.world.item.NovaItem

data class Model(
    val itemStack: ItemStack?,
    val location: Location,
    val billboardConstraints: Billboard = Billboard.FIXED,
    val translation: Vector3f = Vector3f(),
    val scale: Vector3f = Vector3f(1f, 1f, 1f),
    val leftRotation: Quaternionf = Quaternionf(),
    val rightRotation: Quaternionf = Quaternionf(),
    val brightness: Brightness? = null,
    val width: Float = 0f,
    val height: Float = 0f,
    val glowColor: Int = -1
) {
    
    constructor(
        item: NovaItem,
        location: Location,
        modelId: Int,
        constraints: Billboard = Billboard.FIXED,
        translation: Vector3f = Vector3f(),
        scale: Vector3f = Vector3f(1f, 1f, 1f),
        leftRotation: Quaternionf = Quaternionf(),
        rightRotation: Quaternionf = Quaternionf(),
        brightness: Brightness? = null,
        width: Float = 0f,
        height: Float = 0f,
        glowColor: Int = -1
    ) : this(item.model.unnamedClientsideProviders[modelId].get(), location, constraints, translation, scale, leftRotation, rightRotation, brightness, width, height, glowColor)
    
    constructor(
        item: NovaItem,
        location: Location,
        modelId: String = "default",
        constraints: Billboard = Billboard.FIXED,
        translation: Vector3f = Vector3f(),
        scale: Vector3f = Vector3f(1f, 1f, 1f),
        leftRotation: Quaternionf = Quaternionf(),
        rightRotation: Quaternionf = Quaternionf(),
        brightness: Brightness? = null,
        width: Float = 0f,
        height: Float = 0f,
        glowColor: Int = -1
    ) : this(item.model.clientsideProviders[modelId]?.get(), location, constraints, translation, scale, leftRotation, rightRotation, brightness, width, height, glowColor)
    
    fun createFakeItemDisplay(autoRegister: Boolean = true): FakeItemDisplay =
        FakeItemDisplay(location, autoRegister) { _, data -> applyMetadata(data) }
    
    fun applyMetadata(data: ItemDisplayMetadata) {
        data.itemStack = itemStack
        data.billboardConstraints = billboardConstraints
        data.translation = translation
        data.scale = scale
        data.leftRotation = leftRotation
        data.rightRotation = rightRotation
        data.brightness = brightness
        data.width = width
        data.height = height
        data.glowColor = glowColor
        
        if (data.glowColor != -1)
            data.isGlowing = true
    }
    
}