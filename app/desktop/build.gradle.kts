import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.rpc)
}

val frontend = project(":app:frontend").layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
val embedScript = rootProject.layout.projectDirectory.file("scripts/embed-frontend.mjs")
val nativeDependencies = rootProject.layout.buildDirectory.dir("native-deps")
val pack = tasks.register<Exec>("packFrontend") {
    dependsOn(":app:frontend:wasmJsBrowserDistribution")
    inputs.dir(frontend)
    inputs.file(embedScript)
    val archive = layout.buildDirectory.file("embedded/frontend.pack")
    outputs.file(archive)
    commandLine("node", embedScript.asFile, "pack", frontend.get().asFile, archive.get().asFile)
}

kotlin {
    macosArm64()
    mingwX64()
    linuxX64()
    linuxArm64()
    targets.withType<KotlinNativeTarget>().configureEach {
        val targetName = name
        val title = targetName.replaceFirstChar { it.uppercase() }
        val objectFile = layout.buildDirectory.file("embedded/$targetName/frontend.o")
        val embed = tasks.register<Exec>("embed${title}Frontend") {
            dependsOn(pack)
            val archive = layout.buildDirectory.file("embedded/frontend.pack")
            val compiler = nativeDependencies.get().file("clang")
            inputs.file(archive)
            inputs.file(embedScript)
            inputs.file(compiler)
            inputs.property("target", targetName)
            outputs.file(objectFile)
            commandLine("node", embedScript.asFile, "embed", archive.get().asFile,
                objectFile.get().asFile, targetName, compiler.asFile)
        }
        binaries.executable {
            baseName = "Kauri"
            entryPoint = "dev.kauri.host.main"
        }
        binaries.all {
            if (targetName.startsWith("linux")) {
                val sdk = nativeDependencies.get().dir(targetName).dir("lib")
                linkerOpts("-L${sdk.asFile.absolutePath}")
                linkTaskProvider.configure { inputs.dir(sdk) }
            }
            if (targetName == "mingwX64") {
                linkerOpts(
                    "-ladvapi32", "-lole32", "-lshell32", "-luser32",
                )
            }
        }
        binaries.withType<Executable>().configureEach {
            linkerOpts(objectFile.get().asFile.absolutePath)
            if (targetName == "mingwX64") linkerOpts("-Wl,--subsystem,windows")
            linkTaskProvider.configure {
                dependsOn(embed)
                inputs.file(objectFile)
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":app:shared"))
            implementation(project(":framework"))
            implementation(libs.rpc.server)
            implementation(libs.rpc.json)
            implementation(libs.serialization.json)
            implementation(libs.coroutines)
            implementation(libs.ktor.server)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
