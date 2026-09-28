plugins {
    id("nova.hook-conventions")
}

dependencies {
    compileOnly("com.intellectualsites.plotsquared:plotsquared-core:7.5.13")
    compileOnly("com.intellectualsites.plotsquared:plotsquared-bukkit:7.5.13") { isTransitive = false }
}
