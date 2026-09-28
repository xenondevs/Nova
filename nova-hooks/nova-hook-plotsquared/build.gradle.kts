plugins {
    id("nova.hook-conventions")
}

dependencies {
    compileOnly("com.intellectualsites.plotsquared:plotsquared-core:7.6.0")
    compileOnly("com.intellectualsites.plotsquared:plotsquared-bukkit:7.6.0") { isTransitive = false }
}
