package xyz.xenondevs.nova.resources.builder.model

import org.joml.Matrix4d
import org.joml.Matrix4dc
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import xyz.xenondevs.nova.resources.builder.model.transform.BuildAction
import xyz.xenondevs.nova.resources.builder.model.transform.CombinationAction
import xyz.xenondevs.nova.resources.builder.model.transform.ContextualModelBuildAction
import xyz.xenondevs.nova.resources.builder.model.transform.NonContextualModelBuildAction
import xyz.xenondevs.nova.resources.builder.model.transform.RotationTransform
import xyz.xenondevs.nova.resources.builder.model.transform.ScaleTransform
import xyz.xenondevs.nova.resources.builder.model.transform.Transform
import xyz.xenondevs.nova.resources.builder.model.transform.TranslationTransform
import xyz.xenondevs.nova.resources.builder.task.model.ModelContent
import kotlin.math.min

/**
 * The minimum "from" position that an element can have.
 */
private const val MIN_ELEMENT_FROM = -16.0

/**
 * The maximum "to" position that an element can have.
 */
private const val MAX_ELEMENT_TO = 32.0

/**
 * The maximum bounds that are allowed in models.
 */
private val BOUNDS: AABBdc = AABBd(MIN_ELEMENT_FROM, MIN_ELEMENT_FROM, MIN_ELEMENT_FROM, MAX_ELEMENT_TO, MAX_ELEMENT_TO, MAX_ELEMENT_TO)

/**
 * The model center.
 */
private val CENTER: Vector3dc = Vector3d(8.0, 8.0, 8.0)

/**
 * The result of a [ModelBuilder] build operation.
 *
 * @param model The resulting model
 * @param scale The scale the model needs to be displayed at to achieve the desired size
 */
internal data class ScaledModel(val model: Model, val scale: Double)

/**
 * Builds a model by applying transformations such as rotations, translations, scaling and combinations to a base model.
 */
class ModelBuilder(private val base: Model) {
    
    internal val actions = ArrayList<BuildAction>()
    
    private lateinit var result: Model
    private lateinit var scaledResult: ScaledModel
    private lateinit var displayEntityResult: List<Pair<Model, Matrix4dc>>
    private lateinit var blockStateVariantResult: Pair<Model, Vector2ic>
    
    private fun action(action: BuildAction): ModelBuilder {
        require(!::result.isInitialized) { "ModelBuilder is frozen" }
        actions += action
        return this
    }
    
    /**
     * Rotates the model by the given euler angle in degrees [angle] around the given [axis]
     * through the given [pivot] point.
     *
     * The [uvLock] parameter specifies whether the texture should be rotated with the model.
     *
     * The [rescale] parameter specifies whether the model should be rescaled to fit the new size.
     */
    fun rotate(pivot: Vector3dc, axis: Model.Axis, angle: Double, uvLock: Boolean = false, rescale: Boolean = false): ModelBuilder =
        action(RotationTransform(pivot, axis, angle, uvLock, rescale))
    
    /**
     * Rotates the model by the given euler angle in degrees [angle] around the given [axis]
     * through the pivot point (8, 8, 8).
     *
     * The [uvLock] parameter specifies whether the texture should be rotated with the model.
     *
     * The [rescale] parameter specifies whether the model should be rescaled to fit the new size.
     */
    fun rotate(axis: Model.Axis, angle: Double, uvLock: Boolean = false, rescale: Boolean = false): ModelBuilder =
        rotate(CENTER, axis, angle, uvLock, rescale)
    
    /**
     * Rotates the model by the given euler angle in degrees [angle] around the X axis
     * through the given [pivot] point.
     *
     * The [uvLock] parameter specifies whether the texture should be rotated with the model.
     *
     * The [rescale] parameter specifies whether the model should be rescaled to fit the new size.
     */
    fun rotateX(pivot: Vector3dc, angle: Double, uvLock: Boolean = false, rescale: Boolean = false): ModelBuilder =
        rotate(pivot, Model.Axis.X, angle, uvLock, rescale)
    
    /**
     * Rotates the model by the given euler [angle] in degrees around the X axis
     * through the pivot point (8, 8, 8).
     *
     * The [uvLock] parameter specifies whether the texture should be rotated with the model.
     *
     * The [rescale] parameter specifies whether the model should be rescaled to fit the new size.
     */
    fun rotateX(angle: Double, uvLock: Boolean = false, rescale: Boolean = false): ModelBuilder =
        rotateX(CENTER, angle, uvLock, rescale)
    
    /**
     * Rotates the model by the given euler angle in degrees [angle] around the Y axis
     * through the given [pivot] point.
     *
     * The [uvLock] parameter specifies whether the texture should be rotated with the model.
     *
     * The [rescale] parameter specifies whether the model should be rescaled to fit the new size.
     */
    fun rotateY(pivot: Vector3dc, angle: Double, uvLock: Boolean = false, rescale: Boolean = false): ModelBuilder =
        rotate(pivot, Model.Axis.Y, angle, uvLock, rescale)
    
    /**
     * Rotates the model by the given euler [angle] in degrees around the Y axis
     * through the pivot point (8, 8, 8).
     *
     * The [uvLock] parameter specifies whether the texture should be rotated with the model.
     *
     * The [rescale] parameter specifies whether the model should be rescaled to fit the new size.
     */
    fun rotateY(angle: Double, uvLock: Boolean = false, rescale: Boolean = false): ModelBuilder =
        rotateY(CENTER, angle, uvLock, rescale)
    
    /**
     * Rotates the model by the given euler angle in degrees [angle] around the Z axis
     * through the given [pivot] point.
     *
     * The [uvLock] parameter specifies whether the texture should be rotated with the model.
     *
     * The [rescale] parameter specifies whether the model should be rescaled to fit the new size.
     */
    fun rotateZ(pivot: Vector3dc, angle: Double, uvLock: Boolean = false, rescale: Boolean = false): ModelBuilder =
        rotate(pivot, Model.Axis.Z, angle, uvLock, rescale)
    
    /**
     * Rotates the model by the given euler [angle] in degrees around the Z axis
     * through the pivot point (8, 8, 8).
     *
     * The [uvLock] parameter specifies whether the texture should be rotated with the model.
     *
     * The [rescale] parameter specifies whether the model should be rescaled to fit the new size.
     */
    fun rotateZ(angle: Double, uvLock: Boolean = false, rescale: Boolean = false): ModelBuilder =
        rotateZ(CENTER, angle, uvLock, rescale)
    
    /**
     * Translates the model by the given [offset] vector.
     */
    fun translate(offset: Vector3dc): ModelBuilder =
        action(TranslationTransform(offset))
    
    /**
     * Scales the model by [scale] to the given [pivot] point.
     *
     * With [scaleUV], the UV coordinates are scaled accordingly.
     */
    fun scale(pivot: Vector3dc, scale: Vector3dc, scaleUV: Boolean = false): ModelBuilder =
        action(ScaleTransform(pivot, scale, scaleUV))
    
    /**
     * Scales the model by [scale].
     *
     * With [scaleUV], the UV coordinates are scaled accordingly.
     */
    fun scale(scale: Vector3dc, scaleUV: Boolean = false): ModelBuilder =
        scale(CENTER, scale, scaleUV)
    
    /**
     * Scales the model by the given factor [scale] for all axes to the given [pivot] point.
     *
     * With [scaleUV], the UV coordinates are scaled accordingly.
     */
    fun scale(pivot: Vector3dc, scale: Double, scaleUV: Boolean = false): ModelBuilder =
        scale(pivot, Vector3d(scale, scale, scale), scaleUV)
    
    /**
     * Scales the model by the given factor [scale] for all axes to the pivot point (8, 8, 8).
     *
     * With [scaleUV], the UV coordinates are scaled accordingly.
     */
    fun scale(scale: Double, scaleUV: Boolean = false): ModelBuilder =
        scale(Vector3d(scale, scale, scale), scaleUV)
    
    /**
     * Combines this model with the given [model] by flattening the elements of the other [model] and
     * adding them to this model.
     */
    fun add(model: ModelBuilder): ModelBuilder =
        action(CombinationAction(model))
    
    /**
     * Builds the model according to the configured [actions].
     *
     * The [context] parameter is only required if [ContextualModelBuildActions][ContextualModelBuildAction] are used.
     */
    fun build(context: ModelContent?): Model {
        if (::result.isInitialized)
            return result
        
        val result = build(context, actions)
        this.result = result
        return result
    }
    
    /**
     * Builds the model according to the given [actions].
     *
     * The [context] parameter is only required if [ContextualModelBuildActions][ContextualModelBuildAction] are used.
     */
    private fun build(context: ModelContent?, actions: List<BuildAction>): Model {
        var resultModel = base
        for (action in actions) {
            when (action) {
                is NonContextualModelBuildAction -> resultModel = action.apply(resultModel)
                is ContextualModelBuildAction -> resultModel = action.apply(
                    resultModel,
                    context ?: throw IllegalArgumentException("Contextual action requires context")
                )
            }
        }
        
        this.result = resultModel
        return result
    }
    
    /**
     * Builds the model according to the given [actions] and applies an additional scaling transformation
     * with pivot (8, 8, 8) in case the model elements exceed the maximum size limit.
     * Then returns the model along with a scale factor that needs to be applied to the display-entity transform
     * to achieve the initially desired size.
     *
     * The [context] parameter is only required if [ContextualModelBuildActions][ContextualModelBuildAction] are used.
     */
    internal fun buildScaled(context: ModelContent?): ScaledModel {
        if (::scaledResult.isInitialized)
            return scaledResult
        
        this.scaledResult = rescaled(build(context), context)
        return scaledResult
    }
    
    private fun rescaled(model: Model, context: ModelContent?): ScaledModel {
        var resultModel = model
        
        var scale = 1.0
        val elements = resultModel.elements
        if (!elements.isNullOrEmpty()) {
            val pivot = Vector3d(8.0, 8.0, 8.0)
            val bounds = resultModel.getBounds(context)
            
            if (bounds.maxX > MAX_ELEMENT_TO)
                scale = min(scale, (MAX_ELEMENT_TO - pivot.x) / (bounds.maxX - pivot.x))
            if (bounds.maxY > MAX_ELEMENT_TO)
                scale = min(scale, (MAX_ELEMENT_TO - pivot.y) / (bounds.maxY - pivot.y))
            if (bounds.maxZ > MAX_ELEMENT_TO)
                scale = min(scale, (MAX_ELEMENT_TO - pivot.z) / (bounds.maxZ - pivot.z))
            if (bounds.minX < MIN_ELEMENT_FROM)
                scale = min(scale, (pivot.x - MIN_ELEMENT_FROM) / (pivot.x - bounds.minX))
            if (bounds.minY < MIN_ELEMENT_FROM)
                scale = min(scale, (pivot.y - MIN_ELEMENT_FROM) / (pivot.y - bounds.minY))
            if (bounds.minZ < MIN_ELEMENT_FROM)
                scale = min(scale, (pivot.z - MIN_ELEMENT_FROM) / (pivot.z - bounds.minZ))
            
            resultModel = ScaleTransform(pivot, Vector3d(scale, scale, scale), keepDisplaySize = true).apply(resultModel)
        }
        
        return ScaledModel(resultModel, 1 / scale)
    }
    
    /**
     * Builds the display entity layout for the model according to the configured [actions], also
     * applying an additional scaling transformation with pivot (8, 8, 8) in case the model elements exceed the maximum size limit.
     */
    internal fun buildDisplayEntity(context: ModelContent?): List<Pair<Model, Matrix4dc>> {
        if (::displayEntityResult.isInitialized)
            return displayEntityResult
        
        val models = ArrayList<Pair<Model, Matrix4d>>()
        
        val base = rescaled(base, context)
        models += base.model to Matrix4d().rotateY(Math.PI).scale(base.scale)
        
        for (action in actions) {
            when (action) {
                is Transform -> models.forEach { (_, matrix) -> action.apply(matrix) }
                is CombinationAction -> models += action.other.buildDisplayEntity(context)
                    .map { (model, matrix) -> model to Matrix4d(matrix) }
            }
        }
        
        this.displayEntityResult = models
        return models
    }
    
    /**
     * Builds the block state variant layout for the model according to the configured [actions].
     */
    internal fun buildBlockStateVariant(context: ModelContent?): Pair<Model, Vector2ic> {
        if (::blockStateVariantResult.isInitialized)
            return blockStateVariantResult
        
        val rotations = Vector2i()
        
        val actions = ArrayList(actions)
        val revIt = actions.asReversed().iterator()
        while (revIt.hasNext()) {
            val tf = revIt.next()
            when {
                tf is RotationTransform && tf.axis == Model.Axis.X && tf.rot % 90 == 0.0 ->
                    rotations.add(tf.rot.toInt(), 0)
                
                tf is RotationTransform && tf.axis == Model.Axis.Y && tf.rot % 90 == 0.0 ->
                    rotations.add(0, -tf.rot.toInt())
                
                else -> break
            }
            revIt.remove()
        }
        
        val resultModel = build(context, actions) // build without rotations
        if (!BOUNDS.containsAABB(resultModel.getBounds(context)))
            throw IllegalStateException("Model bounds exceed the allowed range")
        val result = resultModel to rotations
        this.blockStateVariantResult = result
        return result
    }
    
}