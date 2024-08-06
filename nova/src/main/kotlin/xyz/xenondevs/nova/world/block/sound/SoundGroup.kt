@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.block.sound

import net.minecraft.world.level.block.SoundType
import org.bukkit.Location
import org.bukkit.block.Block
import xyz.xenondevs.nova.util.center
import org.bukkit.SoundGroup as BukkitSoundGroup

data class SoundGroup(
    val volume: Float,
    val pitch: Float,
    val breakSound: String,
    val stepSound: String,
    val placeSound: String,
    val hitSound: String,
    val fallSound: String
) {
    
    val breakVolume: Float
        get() = (volume + 1f) / 2f
    val breakPitch: Float
        get() = pitch * 0.8f
    val stepVolume: Float
        get() = volume * 0.15f
    val stepPitch: Float
        get() = pitch
    val placeVolume: Float
        get() = (volume + 1f) / 2f
    val placePitch: Float
        get() = pitch * 0.8f
    val hitVolume: Float
        get() = (volume + 1f) / 8f
    val hitPitch: Float
        get() = pitch * 0.5f
    val fallVolume: Float
        get() = volume * 0.5f
    val fallPitch: Float
        get() = pitch * 0.75f
    
    fun playBreakSound(block: Block) {
        playBreakSound(block.center)
    }
    
    fun playBreakSound(location: Location) {
        location.world!!.playSound(location, breakSound, breakVolume, breakPitch)
    }
    
    fun playStepSound(block: Block) {
        playStepSound(block.center)
    }
    
    fun playStepSound(location: Location) {
        location.world!!.playSound(location, stepSound, stepVolume, stepPitch)
    }
    
    fun playPlaceSound(block: Block) {
        playPlaceSound(block.center)
    }
    
    fun playPlaceSound(location: Location) {
        location.world!!.playSound(location, placeSound, placeVolume, placePitch)
    }
    
    fun playHitSound(block: Block) {
        playHitSound(block.center)
    }
    
    fun playHitSound(location: Location) {
        location.world!!.playSound(location, hitSound, hitVolume, hitPitch)
    }
    
    fun playFallSound(block: Block) {
        playFallSound(block.center)
    }
    
    fun playFallSound(location: Location) {
        location.world!!.playSound(location, fallSound, fallVolume, fallPitch)
    }
    
    companion object {
        
        //<editor-fold desc="vanilla sound groups", defaultstate="collapsed">
        val WOOD: SoundGroup = from(SoundType.WOOD)
        val GRAVEL: SoundGroup = from(SoundType.GRAVEL)
        val GRASS: SoundGroup = from(SoundType.GRASS)
        val LILY_PAD: SoundGroup = from(SoundType.LILY_PAD)
        val STONE: SoundGroup = from(SoundType.STONE)
        val METAL: SoundGroup = from(SoundType.METAL)
        val GLASS: SoundGroup = from(SoundType.GLASS)
        val WOOL: SoundGroup = from(SoundType.WOOL)
        val SAND: SoundGroup = from(SoundType.SAND)
        val SNOW: SoundGroup = from(SoundType.SNOW)
        val POWDER_SNOW: SoundGroup = from(SoundType.POWDER_SNOW)
        val LADDER: SoundGroup = from(SoundType.LADDER)
        val ANVIL: SoundGroup = from(SoundType.ANVIL)
        val SLIME_BLOCK: SoundGroup = from(SoundType.SLIME_BLOCK)
        val HONEY_BLOCK: SoundGroup = from(SoundType.HONEY_BLOCK)
        val WET_GRASS: SoundGroup = from(SoundType.WET_GRASS)
        val CORAL_BLOCK: SoundGroup = from(SoundType.CORAL_BLOCK)
        val BAMBOO: SoundGroup = from(SoundType.BAMBOO)
        val BAMBOO_SAPLING: SoundGroup = from(SoundType.BAMBOO_SAPLING)
        val SCAFFOLDING: SoundGroup = from(SoundType.SCAFFOLDING)
        val SWEET_BERRY_BUSH: SoundGroup = from(SoundType.SWEET_BERRY_BUSH)
        val CROP: SoundGroup = from(SoundType.CROP)
        val HARD_CROP: SoundGroup = from(SoundType.HARD_CROP)
        val VINE: SoundGroup = from(SoundType.VINE)
        val NETHER_WART: SoundGroup = from(SoundType.NETHER_WART)
        val LANTERN: SoundGroup = from(SoundType.LANTERN)
        val STEM: SoundGroup = from(SoundType.STEM)
        val NYLIUM: SoundGroup = from(SoundType.NYLIUM)
        val FUNGUS: SoundGroup = from(SoundType.FUNGUS)
        val ROOTS: SoundGroup = from(SoundType.ROOTS)
        val SHROOMLIGHT: SoundGroup = from(SoundType.SHROOMLIGHT)
        val WEEPING_VINES: SoundGroup = from(SoundType.WEEPING_VINES)
        val TWISTING_VINES: SoundGroup = from(SoundType.TWISTING_VINES)
        val SOUL_SAND: SoundGroup = from(SoundType.SOUL_SAND)
        val SOUL_SOIL: SoundGroup = from(SoundType.SOUL_SOIL)
        val BASALT: SoundGroup = from(SoundType.BASALT)
        val WART_BLOCK: SoundGroup = from(SoundType.WART_BLOCK)
        val NETHERRACK: SoundGroup = from(SoundType.NETHERRACK)
        val NETHER_BRICKS: SoundGroup = from(SoundType.NETHER_BRICKS)
        val NETHER_SPROUTS: SoundGroup = from(SoundType.NETHER_SPROUTS)
        val NETHER_ORE: SoundGroup = from(SoundType.NETHER_ORE)
        val BONE_BLOCK: SoundGroup = from(SoundType.BONE_BLOCK)
        val NETHERITE_BLOCK: SoundGroup = from(SoundType.NETHERITE_BLOCK)
        val ANCIENT_DEBRIS: SoundGroup = from(SoundType.ANCIENT_DEBRIS)
        val LODESTONE: SoundGroup = from(SoundType.LODESTONE)
        val CHAIN: SoundGroup = from(SoundType.CHAIN)
        val NETHER_GOLD_ORE: SoundGroup = from(SoundType.NETHER_GOLD_ORE)
        val GILDED_BLACKSTONE: SoundGroup = from(SoundType.GILDED_BLACKSTONE)
        val CANDLE: SoundGroup = from(SoundType.CANDLE)
        val AMETHYST: SoundGroup = from(SoundType.AMETHYST)
        val AMETHYST_CLUSTER: SoundGroup = from(SoundType.AMETHYST_CLUSTER)
        val SMALL_AMETHYST_BUD: SoundGroup = from(SoundType.SMALL_AMETHYST_BUD)
        val MEDIUM_AMETHYST_BUD: SoundGroup = from(SoundType.MEDIUM_AMETHYST_BUD)
        val LARGE_AMETHYST_BUD: SoundGroup = from(SoundType.LARGE_AMETHYST_BUD)
        val TUFF: SoundGroup = from(SoundType.TUFF)
        val CALCITE: SoundGroup = from(SoundType.CALCITE)
        val DRIPSTONE_BLOCK: SoundGroup = from(SoundType.DRIPSTONE_BLOCK)
        val POINTED_DRIPSTONE: SoundGroup = from(SoundType.POINTED_DRIPSTONE)
        val COPPER: SoundGroup = from(SoundType.COPPER)
        val CAVE_VINES: SoundGroup = from(SoundType.CAVE_VINES)
        val SPORE_BLOSSOM: SoundGroup = from(SoundType.SPORE_BLOSSOM)
        val AZALEA: SoundGroup = from(SoundType.AZALEA)
        val FLOWERING_AZALEA: SoundGroup = from(SoundType.FLOWERING_AZALEA)
        val MOSS_CARPET: SoundGroup = from(SoundType.MOSS_CARPET)
        val MOSS: SoundGroup = from(SoundType.MOSS)
        val BIG_DRIPLEAF: SoundGroup = from(SoundType.BIG_DRIPLEAF)
        val SMALL_DRIPLEAF: SoundGroup = from(SoundType.SMALL_DRIPLEAF)
        val ROOTED_DIRT: SoundGroup = from(SoundType.ROOTED_DIRT)
        val HANGING_ROOTS: SoundGroup = from(SoundType.HANGING_ROOTS)
        val AZALEA_LEAVES: SoundGroup = from(SoundType.AZALEA_LEAVES)
        val SCULK_SENSOR: SoundGroup = from(SoundType.SCULK_SENSOR)
        val SCULK_CATALYST: SoundGroup = from(SoundType.SCULK_CATALYST)
        val SCULK: SoundGroup = from(SoundType.SCULK)
        val SCULK_VEIN: SoundGroup = from(SoundType.SCULK_VEIN)
        val SCULK_SHRIEKER: SoundGroup = from(SoundType.SCULK_SHRIEKER)
        val GLOW_LICHEN: SoundGroup = from(SoundType.GLOW_LICHEN)
        val DEEPSLATE: SoundGroup = from(SoundType.DEEPSLATE)
        val DEEPSLATE_BRICKS: SoundGroup = from(SoundType.DEEPSLATE_BRICKS)
        val DEEPSLATE_TILES: SoundGroup = from(SoundType.DEEPSLATE_TILES)
        val POLISHED_DEEPSLATE: SoundGroup = from(SoundType.POLISHED_DEEPSLATE)
        val FROGLIGHT: SoundGroup = from(SoundType.FROGLIGHT)
        val FROGSPAWN: SoundGroup = from(SoundType.FROGSPAWN)
        val MANGROVE_ROOTS: SoundGroup = from(SoundType.MANGROVE_ROOTS)
        val MUDDY_MANGROVE_ROOTS: SoundGroup = from(SoundType.MUDDY_MANGROVE_ROOTS)
        val MUD: SoundGroup = from(SoundType.MUD)
        val MUD_BRICKS: SoundGroup = from(SoundType.MUD_BRICKS)
        val PACKED_MUD: SoundGroup = from(SoundType.PACKED_MUD)
        val BAMBOO_WOOD: SoundGroup = from(SoundType.BAMBOO_WOOD)
        val BAMBOO_WOOD_HANGING_SIGN: SoundGroup = from(SoundType.BAMBOO_WOOD_HANGING_SIGN)
        val CHERRY_LEAVES: SoundGroup = from(SoundType.CHERRY_LEAVES)
        val CHERRY_SAPLING: SoundGroup = from(SoundType.CHERRY_SAPLING)
        val CHERRY_WOOD: SoundGroup = from(SoundType.CHERRY_WOOD)
        val CHERRY_WOOD_HANGING_SIGN: SoundGroup = from(SoundType.CHERRY_WOOD_HANGING_SIGN)
        val CHISELED_BOOKSHELF: SoundGroup = from(SoundType.CHISELED_BOOKSHELF)
        val COBWEB: SoundGroup = from(SoundType.COBWEB)
        val COPPER_BULB: SoundGroup = from(SoundType.COPPER_BULB)
        val COPPER_GRATE: SoundGroup = from(SoundType.COPPER_GRATE)
        val DECORATED_POT: SoundGroup = from(SoundType.DECORATED_POT)
        val DECORATED_POT_CRACKED: SoundGroup = from(SoundType.DECORATED_POT_CRACKED)
        val HANGING_SIGN: SoundGroup = from(SoundType.HANGING_SIGN)
        val HEAVY_CORE: SoundGroup = from(SoundType.HEAVY_CORE)
        val NETHER_WOOD: SoundGroup = from(SoundType.NETHER_WOOD)
        val NETHER_WOOD_HANGING_SIGN: SoundGroup = from(SoundType.NETHER_WOOD_HANGING_SIGN)
        val PINK_PETALS: SoundGroup = from(SoundType.PINK_PETALS)
        val POLISHED_TUFF: SoundGroup = from(SoundType.POLISHED_TUFF)
        val SPONGE: SoundGroup = from(SoundType.SPONGE)
        val SUSPICIOUS_GRAVEL: SoundGroup = from(SoundType.SUSPICIOUS_GRAVEL)
        val SUSPICIOUS_SAND: SoundGroup = from(SoundType.SUSPICIOUS_SAND)
        val TRIAL_SPAWNER: SoundGroup = from(SoundType.TRIAL_SPAWNER)
        val TUFF_BRICKS: SoundGroup = from(SoundType.TUFF_BRICKS)
        val VAULT: SoundGroup = from(SoundType.VAULT)
        val WET_SPONGE: SoundGroup = from(SoundType.WET_SPONGE)
        //</editor-fold>
        
        fun from(soundGroup: BukkitSoundGroup): SoundGroup {
            return SoundGroup(
                soundGroup.volume,
                soundGroup.pitch,
                soundGroup.breakSound.key.toString(),
                soundGroup.stepSound.key.toString(),
                soundGroup.placeSound.key.toString(),
                soundGroup.hitSound.key.toString(),
                soundGroup.fallSound.key.toString()
            )
        }
        
        fun from(soundType: SoundType): SoundGroup {
            return SoundGroup(
                soundType.volume,
                soundType.pitch,
                soundType.breakSound.location.toString(),
                soundType.stepSound.location.toString(),
                soundType.placeSound.location.toString(),
                soundType.hitSound.location.toString(),
                soundType.fallSound.location.toString()
            )
        }
        
    }
    
}