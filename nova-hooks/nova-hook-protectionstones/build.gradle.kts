plugins {
    id("nova.hook-conventions")
}

dependencies {
    compileOnly("dev.espi:protectionstones:2.10.2") { isTransitive = false }
}