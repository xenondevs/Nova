plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://repo.xenondevs.xyz/third-party-releases/")
}

dependencies {
    compileOnly("org.maxgamer:QuickShop:5.1.0.7") { isTransitive = false }
}