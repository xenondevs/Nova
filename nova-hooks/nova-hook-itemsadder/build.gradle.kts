plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.2.4") { isTransitive = false }
}