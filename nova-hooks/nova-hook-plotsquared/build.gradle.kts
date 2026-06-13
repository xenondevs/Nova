plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.intellectualsites.plotsquared:plotsquared-core:7.5.13")
    compileOnly("com.intellectualsites.plotsquared:plotsquared-bukkit:7.5.13") { isTransitive = false }
}