plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.intellectualsites.plotsquared:plotsquared-core:7.5.10")
    compileOnly("com.intellectualsites.plotsquared:plotsquared-bukkit:7.5.10") { isTransitive = false }
}