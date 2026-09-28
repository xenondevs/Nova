package xyz.xenondevs.nova.packetentity

import com.mojang.math.MatrixUtil
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Vector3f
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.dsl.DslProperty
import xyz.xenondevs.commons.tuple.Tuple4

//<editor-fold desc="Entity flags" defaultstate="collapsed">

val EntityMetadataDsl.isOnFire: DslProperty<Boolean>
    get() = sharedFlags[0]

var EntityMetadata.isOnFire: Boolean
    get() = sharedFlags[0]
    set(value) { sharedFlags[0] = value }

val EntityMetadataDsl.isCrouching: DslProperty<Boolean>
    get() = sharedFlags[1]

var EntityMetadata.isCrouching: Boolean
    get() = sharedFlags[1]
    set(value) { sharedFlags[1] = value }

val EntityMetadataDsl.isSprinting: DslProperty<Boolean>
    get() = sharedFlags[3]

var EntityMetadata.isSprinting: Boolean
    get() = sharedFlags[3]
    set(value) { sharedFlags[3] = value }

val EntityMetadataDsl.isSwimming: DslProperty<Boolean>
    get() = sharedFlags[4]

var EntityMetadata.isSwimming: Boolean
    get() = sharedFlags[4]
    set(value) { sharedFlags[4] = value }

val EntityMetadataDsl.isInvisible: DslProperty<Boolean>
    get() = sharedFlags[5]

var EntityMetadata.isInvisible: Boolean
    get() = sharedFlags[5]
    set(value) { sharedFlags[5] = value }

val EntityMetadataDsl.isGlowing: DslProperty<Boolean>
    get() = sharedFlags[6]

var EntityMetadata.isGlowing: Boolean
    get() = sharedFlags[6]
    set(value) { sharedFlags[6] = value }

val EntityMetadataDsl.isFlyingElytra: DslProperty<Boolean>
    get() = sharedFlags[7]

var EntityMetadata.isFlyingElytra: Boolean
    get() = sharedFlags[7]
    set(value) { sharedFlags[7] = value }

//</editor-fold>

//<editor-fold desc="LivingEntity flags" defaultstate="collapsed">

val LivingEntityMetadataDsl.isUsingItem: DslProperty<Boolean>
    get() = livingEntityFlags[0]

var LivingEntityMetadata.isUsingItem: Boolean
    get() = livingEntityFlags[0]
    set(value) { livingEntityFlags[0] = value }

val LivingEntityMetadataDsl.isOffHandActive: DslProperty<Boolean>
    get() = livingEntityFlags[1]

var LivingEntityMetadata.isOffHandActive: Boolean
    get() = livingEntityFlags[1]
    set(value) { livingEntityFlags[1] = value }

val LivingEntityMetadataDsl.isSpinAttacking: DslProperty<Boolean>
    get() = livingEntityFlags[2]

var LivingEntityMetadata.isSpinAttacking: Boolean
    get() = livingEntityFlags[2]
    set(value) { livingEntityFlags[2] = value }

//</editor-fold>

//<editor-fold desc="Mob flags" defaultstate="collapsed">

val MobMetadataDsl.hasNoAi: DslProperty<Boolean>
    get() = mobFlags[0]

var MobMetadata.hasNoAi: Boolean
    get() = mobFlags[0]
    set(value) { mobFlags[0] = value }

val MobMetadataDsl.isLeftHanded: DslProperty<Boolean>
    get() = mobFlags[1]

var MobMetadata.isLeftHanded: Boolean
    get() = mobFlags[1]
    set(value) { mobFlags[1] = value }

val MobMetadataDsl.isAggressive: DslProperty<Boolean>
    get() = mobFlags[2]

var MobMetadata.isAggressive: Boolean
    get() = mobFlags[2]
    set(value) { mobFlags[2] = value }

//</editor-fold>

//<editor-fold desc="TamableAnimal flags" defaultstate="collapsed">

val TamableAnimalMetadataDsl.isSitting: DslProperty<Boolean>
    get() = flags[0]

var TamableAnimalMetadata.isSitting: Boolean
    get() = flags[0]
    set(value) { flags[0] = value }

val TamableAnimalMetadataDsl.isTamed: DslProperty<Boolean>
    get() = flags[2]

var TamableAnimalMetadata.isTamed: Boolean
    get() = flags[2]
    set(value) { flags[2] = value }

//</editor-fold>

//<editor-fold desc="Bat flags" defaultstate="collapsed">

val BatMetadataDsl.isResting: DslProperty<Boolean>
    get() = flags[0]

var BatMetadata.isResting: Boolean
    get() = flags[0]
    set(value) { flags[0] = value }

//</editor-fold>

//<editor-fold desc="Bee flags" defaultstate="collapsed">

val BeeMetadataDsl.isRolling: DslProperty<Boolean>
    get() = flags[1]

var BeeMetadata.isRolling: Boolean
    get() = flags[1]
    set(value) { flags[1] = value }

val BeeMetadataDsl.hasStung: DslProperty<Boolean>
    get() = flags[2]

var BeeMetadata.hasStung: Boolean
    get() = flags[2]
    set(value) { flags[2] = value }

val BeeMetadataDsl.hasNectar: DslProperty<Boolean>
    get() = flags[3]

var BeeMetadata.hasNectar: Boolean
    get() = flags[3]
    set(value) { flags[3] = value }

//</editor-fold>

//<editor-fold desc="AbstractHorse flags" defaultstate="collapsed">

val AbstractHorseMetadataDsl.isTamed: DslProperty<Boolean>
    get() = flags[1]

var AbstractHorseMetadata.isTamed: Boolean
    get() = flags[1]
    set(value) { flags[1] = value }

val AbstractHorseMetadataDsl.isBred: DslProperty<Boolean>
    get() = flags[3]

var AbstractHorseMetadata.isBred: Boolean
    get() = flags[3]
    set(value) { flags[3] = value }

val AbstractHorseMetadataDsl.isEating: DslProperty<Boolean>
    get() = flags[4]

var AbstractHorseMetadata.isEating: Boolean
    get() = flags[4]
    set(value) { flags[4] = value }

val AbstractHorseMetadataDsl.isRearing: DslProperty<Boolean>
    get() = flags[5]

var AbstractHorseMetadata.isRearing: Boolean
    get() = flags[5]
    set(value) { flags[5] = value }

val AbstractHorseMetadataDsl.isMouthOpen: DslProperty<Boolean>
    get() = flags[6]

var AbstractHorseMetadata.isMouthOpen: Boolean
    get() = flags[6]
    set(value) { flags[6] = value }

//</editor-fold>

//<editor-fold desc="Fox flags" defaultstate="collapsed">

val FoxMetadataDsl.isFoxSitting: DslProperty<Boolean>
    get() = flags[0]

var FoxMetadata.isFoxSitting: Boolean
    get() = flags[0]
    set(value) { flags[0] = value }

val FoxMetadataDsl.isFoxCrouching: DslProperty<Boolean>
    get() = flags[2]

var FoxMetadata.isFoxCrouching: Boolean
    get() = flags[2]
    set(value) { flags[2] = value }

val FoxMetadataDsl.isInterested: DslProperty<Boolean>
    get() = flags[3]

var FoxMetadata.isInterested: Boolean
    get() = flags[3]
    set(value) { flags[3] = value }

val FoxMetadataDsl.isPouncing: DslProperty<Boolean>
    get() = flags[4]

var FoxMetadata.isPouncing: Boolean
    get() = flags[4]
    set(value) { flags[4] = value }

val FoxMetadataDsl.isSleeping: DslProperty<Boolean>
    get() = flags[5]

var FoxMetadata.isSleeping: Boolean
    get() = flags[5]
    set(value) { flags[5] = value }

val FoxMetadataDsl.isFacePlanted: DslProperty<Boolean>
    get() = flags[6]

var FoxMetadata.isFacePlanted: Boolean
    get() = flags[6]
    set(value) { flags[6] = value }

val FoxMetadataDsl.isDefending: DslProperty<Boolean>
    get() = flags[7]

var FoxMetadata.isDefending: Boolean
    get() = flags[7]
    set(value) { flags[7] = value }

//</editor-fold>

//<editor-fold desc="IronGolem flags" defaultstate="collapsed">

val IronGolemMetadataDsl.isPlayerCreated: DslProperty<Boolean>
    get() = flags[0]

var IronGolemMetadata.isPlayerCreated: Boolean
    get() = flags[0]
    set(value) { flags[0] = value }

//</editor-fold>

//<editor-fold desc="Panda flags" defaultstate="collapsed">

val PandaMetadataDsl.isSneezing: DslProperty<Boolean>
    get() = flags[1]

var PandaMetadata.isSneezing: Boolean
    get() = flags[1]
    set(value) { flags[1] = value }

val PandaMetadataDsl.isRolling: DslProperty<Boolean>
    get() = flags[2]

var PandaMetadata.isRolling: Boolean
    get() = flags[2]
    set(value) { flags[2] = value }

val PandaMetadataDsl.isPandaSitting: DslProperty<Boolean>
    get() = flags[3]

var PandaMetadata.isPandaSitting: Boolean
    get() = flags[3]
    set(value) { flags[3] = value }

val PandaMetadataDsl.isOnBack: DslProperty<Boolean>
    get() = flags[4]

var PandaMetadata.isOnBack: Boolean
    get() = flags[4]
    set(value) { flags[4] = value }

//</editor-fold>

//<editor-fold desc="ArmorStand flags" defaultstate="collapsed">

val ArmorStandMetadataDsl.isSmall: DslProperty<Boolean>
    get() = clientFlags[0]

var ArmorStandMetadata.isSmall: Boolean
    get() = clientFlags[0]
    set(value) { clientFlags[0] = value }

val ArmorStandMetadataDsl.hasArms: DslProperty<Boolean>
    get() = clientFlags[2]

var ArmorStandMetadata.hasArms: Boolean
    get() = clientFlags[2]
    set(value) { clientFlags[2] = value }

val ArmorStandMetadataDsl.hasNoBasePlate: DslProperty<Boolean>
    get() = clientFlags[3]

var ArmorStandMetadata.hasNoBasePlate: Boolean
    get() = clientFlags[3]
    set(value) { clientFlags[3] = value }

val ArmorStandMetadataDsl.isMarker: DslProperty<Boolean>
    get() = clientFlags[4]

var ArmorStandMetadata.isMarker: Boolean
    get() = clientFlags[4]
    set(value) { clientFlags[4] = value }

//</editor-fold>

//<editor-fold desc="Blaze flags" defaultstate="collapsed">

val BlazeMetadataDsl.isCharged: DslProperty<Boolean>
    get() = flags[0]

var BlazeMetadata.isCharged: Boolean
    get() = flags[0]
    set(value) { flags[0] = value }

//</editor-fold>

//<editor-fold desc="Vex flags" defaultstate="collapsed">

val VexMetadataDsl.isCharging: DslProperty<Boolean>
    get() = flags[0]

var VexMetadata.isCharging: Boolean
    get() = flags[0]
    set(value) { flags[0] = value }

//</editor-fold>

//<editor-fold desc="Spider flags" defaultstate="collapsed">

val SpiderMetadataDsl.isClimbing: DslProperty<Boolean>
    get() = flags[0]

var SpiderMetadata.isClimbing: Boolean
    get() = flags[0]
    set(value) { flags[0] = value }

//</editor-fold>

//<editor-fold desc="AbstractArrow flags" defaultstate="collapsed">

val AbstractArrowMetadataDsl.isCritical: DslProperty<Boolean>
    get() = flags[0]

var AbstractArrowMetadata.isCritical: Boolean
    get() = flags[0]
    set(value) { flags[0] = value }

val AbstractArrowMetadataDsl.hasNoPhysics: DslProperty<Boolean>
    get() = flags[1]

var AbstractArrowMetadata.hasNoPhysics: Boolean
    get() = flags[1]
    set(value) { flags[1] = value }

//</editor-fold>

//<editor-fold desc="TextDisplay style flags" defaultstate="collapsed">

val TextDisplayMetadataDsl.hasShadow: DslProperty<Boolean>
    get() = styleFlags[0]

var TextDisplayMetadata.hasShadow: Boolean
    get() = styleFlags[0]
    set(value) { styleFlags[0] = value }

val TextDisplayMetadataDsl.isSeeThrough: DslProperty<Boolean>
    get() = styleFlags[1]

var TextDisplayMetadata.isSeeThrough: Boolean
    get() = styleFlags[1]
    set(value) { styleFlags[1] = value }

val TextDisplayMetadataDsl.useDefaultBackground: DslProperty<Boolean>
    get() = styleFlags[2]

var TextDisplayMetadata.useDefaultBackground: Boolean
    get() = styleFlags[2]
    set(value) { styleFlags[2] = value }

val TextDisplayMetadataDsl.isAlignedLeft: DslProperty<Boolean>
    get() = styleFlags[3]

var TextDisplayMetadata.isAlignedLeft: Boolean
    get() = styleFlags[3]
    set(value) { styleFlags[3] = value }

val TextDisplayMetadataDsl.isAlignedRight: DslProperty<Boolean>
    get() = styleFlags[4]

var TextDisplayMetadata.isAlignedRight: Boolean
    get() = styleFlags[4]
    set(value) { styleFlags[4] = value }

//</editor-fold>

val DisplayMetadataDsl.transform: DslProperty<Matrix4fc>
    get() = TransformDslPropertyImpl(this)

var DisplayMetadata.transform: Matrix4fc
    get() = Matrix4f()
        .translationRotateScale(translation, leftRotation, scale)
        .rotate(rightRotation)
    set(value) {
        val translation = Vector3f()
        val leftRotation = Quaternionf()
        val scale = Vector3f()
        val rightRotation = Quaternionf()
        MatrixUtil.svdDecompose(value, translation, leftRotation, scale, rightRotation)
        
        this.translation = translation
        this.leftRotation = leftRotation
        this.scale = scale
        this.rightRotation = rightRotation
    }

internal class TransformDslPropertyImpl(
    private val dsl: DisplayMetadataDsl
) : DslProperty<Matrix4fc> {
    
    override fun by(value: Matrix4fc) {
        val translation = Vector3f()
        val leftRotation = Quaternionf()
        val scale = Vector3f()
        val rightRotation = Quaternionf()
        MatrixUtil.svdDecompose(value, translation, leftRotation, scale, rightRotation)
        
        dsl.translation by translation
        dsl.leftRotation by leftRotation
        dsl.scale by scale
        dsl.rightRotation by rightRotation
    }
    
    override fun by(provider: Provider<Matrix4fc>) {
        val decomposed = provider.map { transform ->
            val translation = Vector3f()
            val leftRotation = Quaternionf()
            val scale = Vector3f()
            val rightRotation = Quaternionf()
            MatrixUtil.svdDecompose(transform, translation, leftRotation, scale, rightRotation)
            
            Tuple4(translation, leftRotation, scale, rightRotation)
        }
        dsl.translation by decomposed.map { it.a }
        dsl.leftRotation by decomposed.map { it.b }
        dsl.scale by decomposed.map { it.c }
        dsl.rightRotation by decomposed.map { it.d }
    }
    
}
