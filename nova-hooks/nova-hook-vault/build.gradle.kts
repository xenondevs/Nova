plugins {
    id("nova.hook-conventions")
}

repositories { 
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
}