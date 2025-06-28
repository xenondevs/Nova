plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://repo.nexomc.com/releases")
}

dependencies {
    compileOnly("com.nexomc:nexo:1.0.0")
}