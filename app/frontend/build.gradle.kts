import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.rpc)
    alias(libs.plugins.compose)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":app:shared"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.rpc.client)
            implementation(libs.rpc.json)
            implementation(libs.ktor.client)
            implementation(libs.browser)
        }
    }
}

val nodeVersion = providers.fileContents(rootProject.layout.projectDirectory.file(".nvmrc")).asText.map { it.trim() }
val nodeDirectory = providers.environmentVariable("NVM_DIR")
    .orElse(providers.systemProperty("user.home").map { "$it/.nvm" })
val nodeCommand = nodeDirectory.zip(nodeVersion) { directory, version ->
    "$directory/versions/node/v$version/bin/node"
}
rootProject.plugins.withType<WasmNodeJsPlugin>().configureEach {
    rootProject.extensions.configure<WasmNodeJsEnvSpec> {
        download.set(false)
        version.set(nodeVersion)
        command.set(nodeCommand)
    }
}
val binaryenVersion = libs.versions.binaryen.get()
plugins.withType<BinaryenPlugin>().configureEach {
    extensions.configure<BinaryenEnvSpec> {
        version.set(binaryenVersion)
        downloadBaseUrl.set(null as String?)
    }
}
