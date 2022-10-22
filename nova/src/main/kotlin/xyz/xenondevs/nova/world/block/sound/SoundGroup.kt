@file:Suppress("unused")

package xyz.xenondevs.nova.world.block.sound

import net.minecraft.world.level.block.SoundType
import org.bukkit.Sound
import xyz.xenondevs.nova.data.NamespacedId
import org.bukkit.SoundGroup as BukkitSoundGroup

interface SoundGroup {
    
    val volume: Float
    val pitch: Float
    val breakSound: String
    val stepSound: String
    val placeSound: String
    val hitSound: String
    val fallSound: String
    
    companion object {
        
        //<editor-fold desc="vanilla sound groups", defaultstate="collapsed">
        val WOOD: SoundGroup = NMSSoundGroupWrapper(SoundType.WOOD)
        val GRAVEL: SoundGroup = NMSSoundGroupWrapper(SoundType.GRAVEL)
        val GRASS: SoundGroup = NMSSoundGroupWrapper(SoundType.GRASS)
        val LILY_PAD: SoundGroup = NMSSoundGroupWrapper(SoundType.LILY_PAD)
        val STONE: SoundGroup = NMSSoundGroupWrapper(SoundType.STONE)
        val METAL: SoundGroup = NMSSoundGroupWrapper(SoundType.METAL)
        val GLASS: SoundGroup = NMSSoundGroupWrapper(SoundType.GLASS)
        val WOOL: SoundGroup = NMSSoundGroupWrapper(SoundType.WOOL)
        val SAND: SoundGroup = NMSSoundGroupWrapper(SoundType.SAND)
        val SNOW: SoundGroup = NMSSoundGroupWrapper(SoundType.SNOW)
        val POWDER_SNOW: SoundGroup = NMSSoundGroupWrapper(SoundType.POWDER_SNOW)
        val LADDER: SoundGroup = NMSSoundGroupWrapper(SoundType.LADDER)
        val ANVIL: SoundGroup = NMSSoundGroupWrapper(SoundType.ANVIL)
        val SLIME_BLOCK: SoundGroup = NMSSoundGroupWrapper(SoundType.SLIME_BLOCK)
        val HONEY_BLOCK: SoundGroup = NMSSoundGroupWrapper(SoundType.HONEY_BLOCK)
        val WET_GRASS: SoundGroup = NMSSoundGroupWrapper(SoundType.WET_GRASS)
        val CORAL_BLOCK: SoundGroup = NMSSoundGroupWrapper(SoundType.CORAL_BLOCK)
        val BAMBOO: SoundGroup = NMSSoundGroupWrapper(SoundType.BAMBOO)
        val BAMBOO_SAPLING: SoundGroup = NMSSoundGroupWrapper(SoundType.BAMBOO_SAPLING)
        val SCAFFOLDING: SoundGroup = NMSSoundGroupWrapper(SoundType.SCAFFOLDING)
        val SWEET_BERRY_BUSH: SoundGroup = NMSSoundGroupWrapper(SoundType.SWEET_BERRY_BUSH)
        val CROP: SoundGroup = NMSSoundGroupWrapper(SoundType.CROP)
        val HARD_CROP: SoundGroup = NMSSoundGroupWrapper(SoundType.HARD_CROP)
        val VINE: SoundGroup = NMSSoundGroupWrapper(SoundType.VINE)
        val NETHER_WART: SoundGroup = NMSSoundGroupWrapper(SoundType.NETHER_WART)
        val LANTERN: SoundGroup = NMSSoundGroupWrapper(SoundType.LANTERN)
        val STEM: SoundGroup = NMSSoundGroupWrapper(SoundType.STEM)
        val NYLIUM: SoundGroup = NMSSoundGroupWrapper(SoundType.NYLIUM)
        val FUNGUS: SoundGroup = NMSSoundGroupWrapper(SoundType.FUNGUS)
        val ROOTS: SoundGroup = NMSSoundGroupWrapper(SoundType.ROOTS)
        val SHROOMLIGHT: SoundGroup = NMSSoundGroupWrapper(SoundType.SHROOMLIGHT)
        val WEEPING_VINES: SoundGroup = NMSSoundGroupWrapper(SoundType.WEEPING_VINES)
        val TWISTING_VINES: SoundGroup = NMSSoundGroupWrapper(SoundType.TWISTING_VINES)
        val SOUL_SAND: SoundGroup = NMSSoundGroupWrapper(SoundType.SOUL_SAND)
        val SOUL_SOIL: SoundGroup = NMSSoundGroupWrapper(SoundType.SOUL_SOIL)
        val BASALT: SoundGroup = NMSSoundGroupWrapper(SoundType.BASALT)
        val WART_BLOCK: SoundGroup = NMSSoundGroupWrapper(SoundType.WART_BLOCK)
        val NETHERRACK: SoundGroup = NMSSoundGroupWrapper(SoundType.NETHERRACK)
        val NETHER_BRICKS: SoundGroup = NMSSoundGroupWrapper(SoundType.NETHER_BRICKS)
        val NETHER_SPROUTS: SoundGroup = NMSSoundGroupWrapper(SoundType.NETHER_SPROUTS)
        val NETHER_ORE: SoundGroup = NMSSoundGroupWrapper(SoundType.NETHER_ORE)
        val BONE_BLOCK: SoundGroup = NMSSoundGroupWrapper(SoundType.BONE_BLOCK)
        val NETHERITE_BLOCK: SoundGroup = NMSSoundGroupWrapper(SoundType.NETHERITE_BLOCK)
        val ANCIENT_DEBRIS: SoundGroup = NMSSoundGroupWrapper(SoundType.ANCIENT_DEBRIS)
        val LODESTONE: SoundGroup = NMSSoundGroupWrapper(SoundType.LODESTONE)
        val CHAIN: SoundGroup = NMSSoundGroupWrapper(SoundType.CHAIN)
        val NETHER_GOLD_ORE: SoundGroup = NMSSoundGroupWrapper(SoundType.NETHER_GOLD_ORE)
        val GILDED_BLACKSTONE: SoundGroup = NMSSoundGroupWrapper(SoundType.GILDED_BLACKSTONE)
        val CANDLE: SoundGroup = NMSSoundGroupWrapper(SoundType.CANDLE)
        val AMETHYST: SoundGroup = NMSSoundGroupWrapper(SoundType.AMETHYST)
        val AMETHYST_CLUSTER: SoundGroup = NMSSoundGroupWrapper(SoundType.AMETHYST_CLUSTER)
        val SMALL_AMETHYST_BUD: SoundGroup = NMSSoundGroupWrapper(SoundType.SMALL_AMETHYST_BUD)
        val MEDIUM_AMETHYST_BUD: SoundGroup = NMSSoundGroupWrapper(SoundType.MEDIUM_AMETHYST_BUD)
        val LARGE_AMETHYST_BUD: SoundGroup = NMSSoundGroupWrapper(SoundType.LARGE_AMETHYST_BUD)
        val TUFF: SoundGroup = NMSSoundGroupWrapper(SoundType.TUFF)
        val CALCITE: SoundGroup = NMSSoundGroupWrapper(SoundType.CALCITE)
        val DRIPSTONE_BLOCK: SoundGroup = NMSSoundGroupWrapper(SoundType.DRIPSTONE_BLOCK)
        val POINTED_DRIPSTONE: SoundGroup = NMSSoundGroupWrapper(SoundType.POINTED_DRIPSTONE)
        val COPPER: SoundGroup = NMSSoundGroupWrapper(SoundType.COPPER)
        val CAVE_VINES: SoundGroup = NMSSoundGroupWrapper(SoundType.CAVE_VINES)
        val SPORE_BLOSSOM: SoundGroup = NMSSoundGroupWrapper(SoundType.SPORE_BLOSSOM)
        val AZALEA: SoundGroup = NMSSoundGroupWrapper(SoundType.AZALEA)
        val FLOWERING_AZALEA: SoundGroup = NMSSoundGroupWrapper(SoundType.FLOWERING_AZALEA)
        val MOSS_CARPET: SoundGroup = NMSSoundGroupWrapper(SoundType.MOSS_CARPET)
        val MOSS: SoundGroup = NMSSoundGroupWrapper(SoundType.MOSS)
        val BIG_DRIPLEAF: SoundGroup = NMSSoundGroupWrapper(SoundType.BIG_DRIPLEAF)
        val SMALL_DRIPLEAF: SoundGroup = NMSSoundGroupWrapper(SoundType.SMALL_DRIPLEAF)
        val ROOTED_DIRT: SoundGroup = NMSSoundGroupWrapper(SoundType.ROOTED_DIRT)
        val HANGING_ROOTS: SoundGroup = NMSSoundGroupWrapper(SoundType.HANGING_ROOTS)
        val AZALEA_LEAVES: SoundGroup = NMSSoundGroupWrapper(SoundType.AZALEA_LEAVES)
        val SCULK_SENSOR: SoundGroup = NMSSoundGroupWrapper(SoundType.SCULK_SENSOR)
        val SCULK_CATALYST: SoundGroup = NMSSoundGroupWrapper(SoundType.SCULK_CATALYST)
        val SCULK: SoundGroup = NMSSoundGroupWrapper(SoundType.SCULK)
        val SCULK_VEIN: SoundGroup = NMSSoundGroupWrapper(SoundType.SCULK_VEIN)
        val SCULK_SHRIEKER: SoundGroup = NMSSoundGroupWrapper(SoundType.SCULK_SHRIEKER)
        val GLOW_LICHEN: SoundGroup = NMSSoundGroupWrapper(SoundType.GLOW_LICHEN)
        val DEEPSLATE: SoundGroup = NMSSoundGroupWrapper(SoundType.DEEPSLATE)
        val DEEPSLATE_BRICKS: SoundGroup = NMSSoundGroupWrapper(SoundType.DEEPSLATE_BRICKS)
        val DEEPSLATE_TILES: SoundGroup = NMSSoundGroupWrapper(SoundType.DEEPSLATE_TILES)
        val POLISHED_DEEPSLATE: SoundGroup = NMSSoundGroupWrapper(SoundType.POLISHED_DEEPSLATE)
        val FROGLIGHT: SoundGroup = NMSSoundGroupWrapper(SoundType.FROGLIGHT)
        val FROGSPAWN: SoundGroup = NMSSoundGroupWrapper(SoundType.FROGSPAWN)
        val MANGROVE_ROOTS: SoundGroup = NMSSoundGroupWrapper(SoundType.MANGROVE_ROOTS)
        val MUDDY_MANGROVE_ROOTS: SoundGroup = NMSSoundGroupWrapper(SoundType.MUDDY_MANGROVE_ROOTS)
        val MUD: SoundGroup = NMSSoundGroupWrapper(SoundType.MUD)
        val MUD_BRICKS: SoundGroup = NMSSoundGroupWrapper(SoundType.MUD_BRICKS)
        val PACKED_MUD: SoundGroup = NMSSoundGroupWrapper(SoundType.PACKED_MUD)
        //</editor-fold>
        
    }
    
}

internal class BukkitSoundGroupWrapper(soundGroup: BukkitSoundGroup) : SoundGroup {
    
    override val volume = soundGroup.volume
    override val pitch = soundGroup.pitch
    override val breakSound = soundGroup.breakSound.key.toString()
    override val stepSound = soundGroup.stepSound.key.toString()
    override val placeSound = soundGroup.placeSound.key.toString()
    override val hitSound = soundGroup.hitSound.key.toString()
    override val fallSound = soundGroup.fallSound.key.toString()
    
}

internal class NMSSoundGroupWrapper(soundType: SoundType) : SoundGroup {
    
    override val volume = soundType.volume
    override val pitch = soundType.pitch
    override val breakSound: String = soundType.breakSound.location.toString()
    override val stepSound: String = soundType.stepSound.location.toString()
    override val placeSound: String = soundType.placeSound.location.toString()
    override val hitSound: String = soundType.hitSound.location.toString()
    override val fallSound: String = soundType.fallSound.location.toString()
    
}

class NovaSoundGroup(
    override val volume: Float,
    override val pitch: Float,
    override val breakSound: String,
    override val stepSound: String,
    override val placeSound: String,
    override val hitSound: String,
    override val fallSound: String
) : SoundGroup {
    
    constructor(
        volume: Float, pitch: Float,
        breakSound: Sound,
        stepSound: Sound,
        placeSound: Sound,
        hitSound: Sound,
        fallSound: Sound
    ) : this(
        volume, pitch,
        breakSound.key.toString(),
        stepSound.key.toString(),
        placeSound.key.toString(),
        hitSound.key.toString(),
        fallSound.key.toString()
    )
    
    constructor(
        volume: Float, pitch: Float,
        breakSound: NamespacedId,
        stepSound: NamespacedId,
        placeSound: NamespacedId,
        hitSound: NamespacedId,
        fallSound: NamespacedId
    ) : this(
        volume, pitch,
        breakSound.toString(),
        stepSound.toString(),
        placeSound.toString(),
        hitSound.toString(),
        fallSound.toString()
    )
    
}