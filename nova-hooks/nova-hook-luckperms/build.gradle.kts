plugins {
    id("nova.hook-conventions")
}

dependencies {
    compileOnly("net.luckperms:api:5.5") { isTransitive = false }
}