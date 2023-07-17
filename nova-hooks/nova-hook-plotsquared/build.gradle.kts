plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.paperweight)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("com.plotsquared:PlotSquared-Core:6.10.5") { isTransitive = false }
    compileOnly("com.plotsquared:PlotSquared-Bukkit:6.10.5") { isTransitive = false }
}