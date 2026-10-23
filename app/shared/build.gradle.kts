plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.rpc)
}

kotlin {
    macosArm64()
    mingwX64()
    linuxX64()
    linuxArm64()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    sourceSets {
        commonMain.dependencies {
            api(libs.rpc.core)
            api(libs.serialization.json)
            api(libs.coroutines)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
