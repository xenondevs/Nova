plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://repo.xenondevs.xyz/third-party-releases/")
}

dependencies {
    compileOnly("com.bekvon:Residence:5.0.1.6") { isTransitive = false }
}