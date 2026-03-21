package xyz.xenondevs.nova.world.item

import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.provider.NULL_PROVIDER
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.registry.RegistryEntry

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
object VanillaEquipment {
    
    val LEATHER = equipment("leather")
    val CHAINMAIL = equipment("chainmail")
    val COPPER = equipment("copper")
    val IRON = equipment("iron")
    val GOLD = equipment("gold")
    val DIAMOND = equipment("diamond")
    val NETHERITE = equipment("netherite")
    val ELYTRA = equipment("elytra")
    
    val ARMADILLO_SCUTE = equipment("armadillo_scute")
    val TURTLE_SCUTE = equipment("turtle_scute")
    val TRADER_LLAMA = equipment("trader_llama")
    
    val BLACK_CARPET = equipment("black_carpet")
    val BLUE_CARPET = equipment("blue_carpet")
    val BROWN_CARPET = equipment("brown_carpet")
    val CYAN_CARPET = equipment("cyan_carpet")
    val GRAY_CARPET = equipment("gray_carpet")
    val GREEN_CARPET = equipment("green_carpet")
    val LIGHT_BLUE_CARPET = equipment("light_blue_carpet")
    val LIGHT_GRAY_CARPET = equipment("light_gray_carpet")
    val LIME_CARPET = equipment("lime_carpet")
    val MAGENTA_CARPET = equipment("magenta_carpet")
    val ORANGE_CARPET = equipment("orange_carpet")
    val PINK_CARPET = equipment("pink_carpet")
    val PURPLE_CARPET = equipment("purple_carpet")
    val RED_CARPET = equipment("red_carpet")
    val WHITE_CARPET = equipment("white_carpet")
    val YELLOW_CARPET = equipment("yellow_carpet")
    
    internal fun equipment(name: String): RegistryEntry.Nova<Equipment> =
        RegistryLoader.enqueueNova(NovaRegistries.INTERNAL_EQUIPMENT, Key.key(name)) { Equipment(it, NULL_PROVIDER) }
    
}