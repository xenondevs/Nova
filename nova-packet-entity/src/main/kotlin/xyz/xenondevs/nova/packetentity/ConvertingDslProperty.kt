package xyz.xenondevs.nova.packetentity

import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.datacomponent.item.PaperResolvableProfile
import io.papermc.paper.world.WeatheringCopperState
import net.kyori.adventure.text.Component
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.core.Holder
import net.minecraft.core.Rotations
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.entity.EntityReference
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariant
import net.minecraft.world.entity.animal.chicken.ChickenVariant
import net.minecraft.world.entity.animal.cow.CowSoundVariant
import net.minecraft.world.entity.animal.cow.CowVariant
import net.minecraft.world.entity.animal.feline.CatSoundVariant
import net.minecraft.world.entity.animal.feline.CatVariant
import net.minecraft.world.entity.animal.frog.FrogVariant
import net.minecraft.world.entity.animal.golem.CopperGolemState
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant
import net.minecraft.world.entity.animal.pig.PigSoundVariant
import net.minecraft.world.entity.animal.pig.PigVariant
import net.minecraft.world.entity.animal.sniffer.Sniffer
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant
import net.minecraft.world.entity.animal.wolf.WolfVariant
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase
import net.minecraft.world.entity.decoration.painting.PaintingVariant
import net.minecraft.world.entity.monster.illager.SpellcasterIllager
import net.minecraft.world.level.block.WeatheringCopper
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Art
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.CraftArt
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.block.data.CraftBlockData
import org.bukkit.craftbukkit.entity.CraftCat
import org.bukkit.craftbukkit.entity.CraftChicken
import org.bukkit.craftbukkit.entity.CraftCow
import org.bukkit.craftbukkit.entity.CraftFrog
import org.bukkit.craftbukkit.entity.CraftPig
import org.bukkit.craftbukkit.entity.CraftWolf
import org.bukkit.craftbukkit.entity.CraftZombieNautilus
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Cat
import org.bukkit.entity.Chicken
import org.bukkit.entity.CopperGolem
import org.bukkit.entity.Cow
import org.bukkit.entity.Display
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Fox
import org.bukkit.entity.Frog
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Llama
import org.bukkit.entity.MushroomCow
import org.bukkit.entity.Panda
import org.bukkit.entity.Parrot
import org.bukkit.entity.Pig
import org.bukkit.entity.Pose
import org.bukkit.entity.Rabbit
import org.bukkit.entity.Salmon
import org.bukkit.entity.Spellcaster
import org.bukkit.entity.Wolf
import org.bukkit.entity.ZombieNautilus
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MainHand
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector3i
import org.joml.Vector3ic
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.dsl.DslProperty
import java.util.*
import kotlin.jvm.optionals.getOrNull
import io.papermc.paper.datacomponent.item.ResolvableProfile as BukkitResolvableProfile
import net.minecraft.network.chat.Component as NmsComponent
import net.minecraft.world.entity.Pose as NmsPose
import net.minecraft.world.entity.animal.armadillo.Armadillo as NmsArmadillo
import net.minecraft.world.item.ItemStack as NmsItemStack
import net.minecraft.world.item.component.ResolvableProfile as NmsResolvableProfile
import org.bukkit.entity.Armadillo as BukkitArmadillo
import org.bukkit.entity.Sniffer as BukkitSniffer

/**
 * Tells the KSP generator to use the annotated [ConvertingDslPropertyFactory]
 * for the [property] of [owner], where [owner] is the simple name of the entity class
 * and [property] the sanitized name of the data property (the same name as used for MetadataDsl generation).
 */
@Target(AnnotationTarget.PROPERTY)
@Repeatable
internal annotation class EntityDataPropertyOverride(
    val owner: String,
    val property: String
)

internal object IdentityDslPropertyFactory {
    fun <T> create(field: DefaultEntityValue<T>) = field
    fun <T> get(field: DefaultEntityValue<T>) = field.get()
    fun <T> set(field: DefaultEntityValue<T>, value: T) = field by value
}

internal class ConvertingDslPropertyFactory<B, N>(
    private val toBukkit: (B) -> N,
    private val toNms: (N) -> B
) {
    
    fun create(field: DefaultEntityValue<B>): DslProperty<N> =
        ConvertingDslProperty(field, toNms)
    
    fun get(field: DefaultEntityValue<B>): N =
        toBukkit(field.get())
    
    fun set(field: DefaultEntityValue<B>, value: N) =
        field by toNms(value)
    
}

internal class ConvertingDslProperty<D, T>(
    private val field: DefaultEntityValue<D>,
    private val toNms: (T) -> D
) : DslProperty<T> {
    
    override fun by(value: T) {
        field by toNms(value)
    }
    
    override fun by(provider: Provider<T>) {
        field by provider.map(toNms)
    }
    
    companion object {
        
        //<editor-fold desc="primitive" defaultstate="collapsed">
        val BYTE = IdentityDslPropertyFactory
        val INT = IdentityDslPropertyFactory
        val FLOAT = IdentityDslPropertyFactory
        val LONG = IdentityDslPropertyFactory
        val BOOLEAN = IdentityDslPropertyFactory
        val STRING = IdentityDslPropertyFactory
        //</editor-fold>
        
        //<editor-fold desc="text component" defaultstate="collapsed">
        val COMPONENT: ConvertingDslPropertyFactory<NmsComponent, Component> =
            ConvertingDslPropertyFactory(PaperAdventure::asAdventure, PaperAdventure::asVanilla)
        
        val OPTIONAL_COMPONENT: ConvertingDslPropertyFactory<Optional<NmsComponent>, Component?> =
            ConvertingDslPropertyFactory(
                toBukkit = { it.getOrNull()?.let(PaperAdventure::asAdventure) },
                toNms = { Optional.ofNullable(it?.let(PaperAdventure::asVanilla)) }
            )
        //</editor-fold>
        
        //<editor-fold desc="item" defaultstate="collapsed">
        val ITEM_STACK: ConvertingDslPropertyFactory<NmsItemStack, ItemStack> =
            ConvertingDslPropertyFactory(CraftItemStack::asBukkitCopy, CraftItemStack::asNMSCopy)
        //</editor-fold>
        
        //<editor-fold desc="block" defaultstate="collapsed">
        val BLOCK_STATE: ConvertingDslPropertyFactory<BlockState, BlockData> =
            ConvertingDslPropertyFactory(
                toBukkit = { CraftBlockData.createData(it) },
                toNms = { (it as CraftBlockData).state }
            )
        
        val OPTIONAL_BLOCK_STATE: ConvertingDslPropertyFactory<Optional<BlockState>, BlockData?> =
            ConvertingDslPropertyFactory(
                toBukkit = { it.getOrNull()?.let { s -> CraftBlockData.createData(s) } },
                toNms = { Optional.ofNullable((it as? CraftBlockData)?.state) }
            )
        
        val BLOCK_POS: ConvertingDslPropertyFactory<BlockPos, Vector3ic> =
            ConvertingDslPropertyFactory(
                toBukkit = { Vector3i(it.x, it.y, it.z) },
                toNms = { BlockPos(it.x(), it.y(), it.z()) }
            )
        
        val OPTIONAL_BLOCK_POS: ConvertingDslPropertyFactory<Optional<BlockPos>, Vector3ic?> =
            ConvertingDslPropertyFactory(
                toBukkit = { it.getOrNull()?.let { p -> Vector3i(p.x, p.y, p.z) } },
                toNms = { Optional.ofNullable(it?.let { p -> BlockPos(p.x(), p.y(), p.z()) }) }
            )
        
        val DIRECTION: ConvertingDslPropertyFactory<Direction, BlockFace> =
            ConvertingDslPropertyFactory(
                toBukkit = { CraftBlock.notchToBlockFace(it) },
                toNms = { CraftBlock.blockFaceToNotch(it)!! }
            )
        //</editor-fold>
        
        //<editor-fold desc="math" defaultstate="collapsed">
        val ROTATIONS: ConvertingDslPropertyFactory<Rotations, Vector3fc> =
            ConvertingDslPropertyFactory(
                toBukkit = { Vector3f(it.x, it.y, it.z) },
                toNms = { Rotations(it.x(), it.y(), it.z()) }
            )
        
        val VECTOR3 = IdentityDslPropertyFactory
        val QUATERNION = IdentityDslPropertyFactory
        //</editor-fold>
        
        //<editor-fold desc="enum" defaultstate="collapsed">
        val POSE: ConvertingDslPropertyFactory<NmsPose, Pose> =
            ConvertingDslPropertyFactory(
                toBukkit = { Pose.entries[it.ordinal] },
                toNms = { NmsPose.entries[it.ordinal] }
            )
        
        val HUMANOID_ARM: ConvertingDslPropertyFactory<HumanoidArm, MainHand> =
            ConvertingDslPropertyFactory(
                toBukkit = { if (it == HumanoidArm.LEFT) MainHand.LEFT else MainHand.RIGHT },
                toNms = { if (it == MainHand.LEFT) HumanoidArm.LEFT else HumanoidArm.RIGHT }
            )
        
        val ARMADILLO_STATE: ConvertingDslPropertyFactory<NmsArmadillo.ArmadilloState, BukkitArmadillo.State> =
            ConvertingDslPropertyFactory(
                toBukkit = { BukkitArmadillo.State.entries[it.ordinal] },
                toNms = { NmsArmadillo.ArmadilloState.entries[it.ordinal] }
            )
        
        val SNIFFER_STATE: ConvertingDslPropertyFactory<Sniffer.State, BukkitSniffer.State> =
            ConvertingDslPropertyFactory(
                toBukkit = { BukkitSniffer.State.entries[it.ordinal] },
                toNms = { Sniffer.State.entries[it.ordinal] }
            )
        
        val WEATHERING_COPPER_STATE: ConvertingDslPropertyFactory<WeatheringCopper.WeatherState, WeatheringCopperState> =
            ConvertingDslPropertyFactory(
                toBukkit = { WeatheringCopperState.entries[it.ordinal] },
                toNms = { WeatheringCopper.WeatherState.entries[it.ordinal] }
            )
        
        val COPPER_GOLEM_STATE: ConvertingDslPropertyFactory<CopperGolemState, CopperGolem.State> =
            ConvertingDslPropertyFactory(
                toBukkit = { CopperGolem.State.entries[it.ordinal] },
                toNms = { CopperGolemState.entries[it.ordinal] }
            )
        
        
        @EntityDataPropertyOverride("Axolotl", "variant")
        val AXOLOTL_VARIANT: ConvertingDslPropertyFactory<Int, org.bukkit.entity.Axolotl.Variant> =
            ConvertingDslPropertyFactory(
                toBukkit = { org.bukkit.entity.Axolotl.Variant.entries[it] },
                toNms = { it.ordinal }
            )
        
        @EntityDataPropertyOverride("MushroomCow", "type")
        val MUSHROOM_COW_VARIANT: ConvertingDslPropertyFactory<Int, MushroomCow.Variant> =
            ConvertingDslPropertyFactory(
                toBukkit = { MushroomCow.Variant.entries[it] },
                toNms = { it.ordinal }
            )
        
        @EntityDataPropertyOverride("Salmon", "type")
        val SALMON_VARIANT: ConvertingDslPropertyFactory<Int, Salmon.Variant> =
            ConvertingDslPropertyFactory(
                toBukkit = { Salmon.Variant.entries[it] },
                toNms = { it.ordinal }
            )
        
        @EntityDataPropertyOverride("Fox", "type")
        val FOX_TYPE: ConvertingDslPropertyFactory<Int, Fox.Type> =
            ConvertingDslPropertyFactory(
                toBukkit = { Fox.Type.entries[it] },
                toNms = { it.ordinal }
            )
        
        @EntityDataPropertyOverride("Llama", "variant")
        val LLAMA_COLOR: ConvertingDslPropertyFactory<Int, Llama.Color> =
            ConvertingDslPropertyFactory(
                toBukkit = { Llama.Color.entries[it] },
                toNms = { it.ordinal }
            )
        
        @EntityDataPropertyOverride("Panda", "mainGene")
        @EntityDataPropertyOverride("Panda", "hiddenGene")
        val PANDA_GENE: ConvertingDslPropertyFactory<Byte, Panda.Gene> =
            ConvertingDslPropertyFactory(
                toBukkit = { Panda.Gene.entries[it.toInt()] },
                toNms = { it.ordinal.toByte() }
            )
        
        @EntityDataPropertyOverride("Parrot", "variant")
        val PARROT_VARIANT: ConvertingDslPropertyFactory<Int, Parrot.Variant> =
            ConvertingDslPropertyFactory(
                toBukkit = { Parrot.Variant.entries[it] },
                toNms = { it.ordinal }
            )
        
        @EntityDataPropertyOverride("Rabbit", "type")
        val RABBIT_TYPE: ConvertingDslPropertyFactory<Int, Rabbit.Type> =
            ConvertingDslPropertyFactory(
                toBukkit = { Rabbit.Type.entries[it] },
                toNms = { it.ordinal }
            )
        
        @EntityDataPropertyOverride("EnderDragon", "phase")
        val ENDER_DRAGON_PHASE: ConvertingDslPropertyFactory<Int, EnderDragon.Phase> =
            ConvertingDslPropertyFactory(
                toBukkit = { EnderDragon.Phase.entries[it] },
                toNms = { EnderDragonPhase.getById(it.ordinal).id }
            )
        
        @EntityDataPropertyOverride("SpellcasterIllager", "spellCasting")
        val SPELLCASTER_SPELL: ConvertingDslPropertyFactory<Byte, Spellcaster.Spell> =
            ConvertingDslPropertyFactory(
                toBukkit = { Spellcaster.Spell.valueOf(SpellcasterIllager.IllagerSpell.byId(it.toInt()).name) },
                toNms = { it.ordinal.toByte() }
            )
        //</editor-fold>
        
        //<editor-fold desc="variant holder" defaultstate="collapsed">
        private fun <N : Any, B> holderVariant(
            toBukkit: (Holder<N>) -> B,
            toNms: (B) -> Holder<N>
        ): ConvertingDslPropertyFactory<Holder<N>, B> = ConvertingDslPropertyFactory(toBukkit, toNms)
        
        val CAT_VARIANT: ConvertingDslPropertyFactory<Holder<CatVariant>, Cat.Type> =
            holderVariant(
                { CraftCat.CraftType.minecraftHolderToBukkit(it) },
                { CraftCat.CraftType.bukkitToMinecraftHolder(it) }
            )
        
        val CAT_SOUND_VARIANT: ConvertingDslPropertyFactory<Holder<CatSoundVariant>, Cat.SoundVariant> =
            holderVariant(
                { CraftCat.CraftSoundVariant.minecraftHolderToBukkit(it) },
                { CraftCat.CraftSoundVariant.bukkitToMinecraftHolder(it) }
            )
        
        val WOLF_VARIANT: ConvertingDslPropertyFactory<Holder<WolfVariant>, Wolf.Variant> =
            holderVariant(
                { CraftWolf.CraftVariant.minecraftHolderToBukkit(it) },
                { CraftWolf.CraftVariant.bukkitToMinecraftHolder(it) }
            )
        
        val WOLF_SOUND_VARIANT: ConvertingDslPropertyFactory<Holder<WolfSoundVariant>, Wolf.SoundVariant> =
            holderVariant(
                { CraftWolf.CraftSoundVariant.minecraftHolderToBukkit(it) },
                { CraftWolf.CraftSoundVariant.bukkitToMinecraftHolder(it) }
            )
        
        val FROG_VARIANT: ConvertingDslPropertyFactory<Holder<FrogVariant>, Frog.Variant> =
            holderVariant(
                { CraftFrog.CraftVariant.minecraftHolderToBukkit(it) },
                { CraftFrog.CraftVariant.bukkitToMinecraftHolder(it) }
            )
        
        val PIG_VARIANT: ConvertingDslPropertyFactory<Holder<PigVariant>, Pig.Variant> =
            holderVariant(
                { CraftPig.CraftVariant.minecraftHolderToBukkit(it) },
                { CraftPig.CraftVariant.bukkitToMinecraftHolder(it) }
            )
        
        val PIG_SOUND_VARIANT: ConvertingDslPropertyFactory<Holder<PigSoundVariant>, Pig.SoundVariant> =
            holderVariant(
                { CraftPig.CraftSoundVariant.minecraftHolderToBukkit(it) },
                { CraftPig.CraftSoundVariant.bukkitToMinecraftHolder(it) }
            )
        
        val CHICKEN_VARIANT: ConvertingDslPropertyFactory<Holder<ChickenVariant>, Chicken.Variant> =
            holderVariant(
                { CraftChicken.CraftVariant.minecraftHolderToBukkit(it) },
                { CraftChicken.CraftVariant.bukkitToMinecraftHolder(it) }
            )
        
        val CHICKEN_SOUND_VARIANT: ConvertingDslPropertyFactory<Holder<ChickenSoundVariant>, Chicken.SoundVariant> =
            holderVariant(
                { CraftChicken.CraftSoundVariant.minecraftHolderToBukkit(it) },
                { CraftChicken.CraftSoundVariant.bukkitToMinecraftHolder(it) }
            )
        
        val COW_VARIANT: ConvertingDslPropertyFactory<Holder<CowVariant>, Cow.Variant> =
            holderVariant(
                { CraftCow.CraftVariant.minecraftHolderToBukkit(it) },
                { CraftCow.CraftVariant.bukkitToMinecraftHolder(it) }
            )
        
        val COW_SOUND_VARIANT: ConvertingDslPropertyFactory<Holder<CowSoundVariant>, Cow.SoundVariant> =
            holderVariant(
                { CraftCow.CraftSoundVariant.minecraftHolderToBukkit(it) },
                { CraftCow.CraftSoundVariant.bukkitToMinecraftHolder(it) }
            )
        
        val ZOMBIE_NAUTILUS_VARIANT: ConvertingDslPropertyFactory<Holder<ZombieNautilusVariant>, ZombieNautilus.Variant> =
            holderVariant(
                { CraftZombieNautilus.CraftVariant.minecraftHolderToBukkit(it) },
                { CraftZombieNautilus.CraftVariant.bukkitToMinecraftHolder(it) }
            )
        //</editor-fold>
        
        //<editor-fold desc="misc" defaultstate="collapsed">
        val OPTIONAL_UNSIGNED_INT: ConvertingDslPropertyFactory<OptionalInt, Int?> =
            ConvertingDslPropertyFactory(
                toBukkit = { if (it.isPresent) it.asInt else null },
                toNms = { if (it != null) OptionalInt.of(it) else OptionalInt.empty() }
            )
        
        val OPTIONAL_LIVING_ENTITY_REFERENCE: ConvertingDslPropertyFactory<Optional<EntityReference<LivingEntity>>, UUID?> =
            ConvertingDslPropertyFactory(
                toBukkit = { it.getOrNull()?.uuid },
                toNms = { uuid -> Optional.ofNullable(uuid?.let { EntityReference.of(it) }) }
            )
        
        val OPTIONAL_GLOBAL_POS: ConvertingDslPropertyFactory<Optional<GlobalPos>, Location?> =
            ConvertingDslPropertyFactory(
                toBukkit = { opt ->
                    opt.getOrNull()?.let { gp ->
                        val world = Bukkit.getWorlds().firstOrNull { (it as CraftWorld).handle.dimension() == gp.dimension() }
                        world?.let { w -> Location(w, gp.pos().x.toDouble(), gp.pos().y.toDouble(), gp.pos().z.toDouble()) }
                    }
                },
                toNms = { loc ->
                    Optional.ofNullable(loc?.let {
                        GlobalPos.of(
                            (it.world as CraftWorld).handle.dimension(),
                            BlockPos(it.blockX, it.blockY, it.blockZ)
                        )
                    })
                }
            )
        
        val RESOLVABLE_PROFILE: ConvertingDslPropertyFactory<NmsResolvableProfile, BukkitResolvableProfile> =
            ConvertingDslPropertyFactory(
                toBukkit = { PaperResolvableProfile(it) },
                toNms = { (it as PaperResolvableProfile).handle }
            )
        
        val PAINTING_VARIANT: ConvertingDslPropertyFactory<Holder<PaintingVariant>, Art> =
            ConvertingDslPropertyFactory(
                toBukkit = { CraftArt.minecraftHolderToBukkit(it) },
                toNms = { CraftArt.bukkitToMinecraftHolder(it) }
            )
        //</editor-fold>
        
        //<editor-fold desc="unhandled (nms passthrough)" defaultstate="collapsed">
        val PARTICLE = IdentityDslPropertyFactory
        val PARTICLES = IdentityDslPropertyFactory
        val VILLAGER_DATA = IdentityDslPropertyFactory
        //</editor-fold>
        
        //<editor-fold desc="specialized overrides" defaultstate="collapsed">
        @EntityDataPropertyOverride("Display", "billboardRenderConstraints")
        val DISPLAY_BILLBOARD: ConvertingDslPropertyFactory<Byte, Display.Billboard> =
            ConvertingDslPropertyFactory(
                toBukkit = { Display.Billboard.entries[it.toInt()] },
                toNms = { it.ordinal.toByte() }
            )
        
        @EntityDataPropertyOverride("Display", "brightnessOverride")
        val DISPLAY_BRIGHTNESS: ConvertingDslPropertyFactory<Int, Display.Brightness?> =
            ConvertingDslPropertyFactory(
                toBukkit = { packed ->
                    if (packed != -1) {
                        Display.Brightness(
                            LightCoordsUtil.block(packed),
                            LightCoordsUtil.sky(packed)
                        )
                    } else null
                },
                toNms = { if (it != null) LightCoordsUtil.pack(it.blockLight, it.skyLight) else -1 }
            )
        
        @EntityDataPropertyOverride("Display", "glowColorOverride")
        @EntityDataPropertyOverride("Arrow", "effectColor")
        val RGB_COLOR: ConvertingDslPropertyFactory<Int, Color?> =
            ConvertingDslPropertyFactory(
                toBukkit = { if (it == -1) null else Color.fromRGB(it and 0xFFFFFF) },
                toNms = { it?.asRGB() ?: -1 }
            )
        
        @EntityDataPropertyOverride("ItemDisplay", "itemDisplay")
        val ITEM_DISPLAY_TRANSFORM: ConvertingDslPropertyFactory<Byte, ItemDisplay.ItemDisplayTransform> =
            ConvertingDslPropertyFactory(
                toBukkit = { ItemDisplay.ItemDisplayTransform.entries[it.toInt()] },
                toNms = { it.ordinal.toByte() }
            )
        
        @EntityDataPropertyOverride("TextDisplay", "backgroundColor")
        val TEXT_DISPLAY_BACKGROUND_COLOR: ConvertingDslPropertyFactory<Int, Color?> =
            ConvertingDslPropertyFactory(
                toBukkit = { if (it == 0x40000000) null else Color.fromARGB(it) },
                toNms = { it?.asARGB() ?: 0x40000000 }
            )
        
        @EntityDataPropertyOverride("Cat", "collarColor")
        @EntityDataPropertyOverride("Wolf", "collarColor")
        val DYE_COLOR_ID: ConvertingDslPropertyFactory<Int, DyeColor> =
            ConvertingDslPropertyFactory(
                toBukkit = { DyeColor.getByWoolData(it.toByte())!! },
                toNms = { it.woolData.toInt() }
            )
        
        @EntityDataPropertyOverride("Shulker", "color")
        val OPTIONAL_DYE_COLOR_ID: ConvertingDslPropertyFactory<Byte, DyeColor?> =
            ConvertingDslPropertyFactory(
                toBukkit = { if (it.toInt() == 16) null else DyeColor.getByWoolData(it) },
                toNms = { it?.woolData ?: 16.toByte() }
            )
        //</editor-fold>
        
    }
    
}

internal data class ReactiveDataValue<D : Any>(
    val value: EntityValue<D>,
    val serializer: EntityDataSerializer<D>
) {
    fun captureAsDataValue(id: Int): SynchedEntityData.DataValue<D> =
        SynchedEntityData.DataValue(id, serializer, value.get())
}
