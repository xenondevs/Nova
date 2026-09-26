plugins {
    id("nova.hook-conventions")
}

dependencies {
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
}
