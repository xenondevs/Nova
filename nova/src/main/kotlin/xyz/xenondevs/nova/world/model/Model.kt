package xyz.xenondevs.nova.world.model

import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display
import net.minecraft.world.item.ItemDisplayContext
import org.bukkit.Location
import org.joml.Quaternionf
import org.joml.Vector3f
import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

data class Model(
    val itemStack: MojangStack,
    val location: Location,
    val billboardConstraints: Display.BillboardConstraints = Display.BillboardConstraints.FIXED,
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
        itemStack: BukkitStack,
        location: Location,
        constraints: Display.BillboardConstraints = Display.BillboardConstraints.FIXED,
        translation: Vector3f = Vector3f(),
        scale: Vector3f = Vector3f(1f, 1f, 1f),
        leftRotation: Quaternionf = Quaternionf(),
        rightRotation: Quaternionf = Quaternionf(),
        brightness: Brightness? = null,
        width: Float = 0f,
        height: Float = 0f,
        glowColor: Int = -1
    ) : this(itemStack.nmsCopy, location, constraints, translation, scale, leftRotation, rightRotation, brightness, width, height, glowColor)
    
    constructor(
        material: NovaItem,
        location: Location,
        subId: Int = 0,
        constraints: Display.BillboardConstraints = Display.BillboardConstraints.FIXED,
        translation: Vector3f = Vector3f(),
        scale: Vector3f = Vector3f(1f, 1f, 1f),
        leftRotation: Quaternionf = Quaternionf(),
        rightRotation: Quaternionf = Quaternionf(),
        brightness: Brightness? = null,
        width: Float = 0f,
        height: Float = 0f,
        glowColor: Int = -1
    ) : this(material.clientsideProviders[subId].get(), location, constraints, translation, scale, leftRotation, rightRotation, brightness, width, height, glowColor)
    
    fun createFakeItemDisplay(autoRegister: Boolean = true): FakeItemDisplay =
        FakeItemDisplay(location, autoRegister) { _, data ->
            data.itemDisplay = ItemDisplayContext.HEAD
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
        }
    
}