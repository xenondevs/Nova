package xyz.xenondevs.nova.ui.waila.info

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.registry.NovaRegistries.WAILA_INFO_PROVIDER
import xyz.xenondevs.nova.registry.NovaRegistries.WAILA_TOOL_ICON_PROVIDER
import xyz.xenondevs.nova.ui.waila.info.impl.CakeWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CampfireWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CandleWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CauldronWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CocoaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.ComparatorWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.CropWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.DaylightDetectorWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.DefaultNovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.DefaultVanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.LanternWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.RailWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.RedstoneLampWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.RepeaterWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.RespawnAnchorWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.SeaPickleWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.impl.SuspiciousSandWailaInfoProvider
import xyz.xenondevs.nova.util.set

@InternalInit(stage = InitializationStage.PRE_WORLD)
private object DefaultWailaProviders {
    
    init {
        register("default_vanilla", DefaultVanillaWailaInfoProvider)
        register("default_nova", DefaultNovaWailaInfoProvider)
        
        register("candle", CandleWailaInfoProvider)
        register("cake", CakeWailaInfoProvider)
        register("cauldron", CauldronWailaInfoProvider)
        register("sea_pickle", SeaPickleWailaInfoProvider)
        register("campfire", CampfireWailaInfoProvider)
        register("crop", CropWailaInfoProvider)
        register("repeater", RepeaterWailaInfoProvider)
        register("comparator", ComparatorWailaInfoProvider)
        register("rail", RailWailaInfoProvider)
        register("respawn_anchor", RespawnAnchorWailaInfoProvider)
        register("lantern", LanternWailaInfoProvider)
        register("daylight_detector", DaylightDetectorWailaInfoProvider)
        register("cocoa", CocoaWailaInfoProvider)
        register("redstone_lamp", RedstoneLampWailaInfoProvider)
        register("suspicious_sand", SuspiciousSandWailaInfoProvider)
    
        register("vanilla", VanillaWailaToolIconProvider)
    }
    
    private fun <T> register(name: String, provider: WailaInfoProvider<T>) {
        val id = ResourceLocation("nova", name)
        WAILA_INFO_PROVIDER[id] = provider
    }
    
    private fun register(name: String, provider: WailaToolIconProvider) {
        val id = ResourceLocation("nova", name)
        WAILA_TOOL_ICON_PROVIDER[id] = provider
    }
    
    
}