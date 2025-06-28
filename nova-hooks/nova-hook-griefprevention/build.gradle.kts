plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.TechFortress:GriefPrevention:16.17.1") { isTransitive = false }
}