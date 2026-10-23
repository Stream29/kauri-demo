import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    macosArm64()
    mingwX64()
    linuxX64()
    linuxArm64()
    val macos = targets.withType<KotlinNativeTarget>().getByName("macosArm64")
    val windows = targets.withType<KotlinNativeTarget>().getByName("mingwX64")
    val linuxX64Target = targets.withType<KotlinNativeTarget>().getByName("linuxX64")
    val linuxArm64Target = targets.withType<KotlinNativeTarget>().getByName("linuxArm64")
    targets.withType<KotlinNativeTarget>().configureEach {
        compilations["main"].cinterops.create("embeddedAssets") {
            definitionFile.set(project.file("src/nativeInterop/cinterop/embedded-assets.def"))
            compilerOpts("-I${project.file("src/nativeInterop/cinterop").absolutePath}")
            tasks.named(interopProcessingTaskName) {
                inputs.file(layout.projectDirectory.file("src/nativeInterop/cinterop/kauri_assets.h"))
            }
        }
    }
    windows.compilations["main"].cinterops.create("windowsWebView2") {
        definitionFile.set(project.file("src/nativeInterop/windows/webview2.def"))
        compilerOpts("-I${project.file("src/nativeInterop/windows").absolutePath}")
        tasks.named(interopProcessingTaskName) {
            inputs.files(
                layout.projectDirectory.file("src/nativeInterop/windows/WebView2.h"),
                layout.projectDirectory.file("src/nativeInterop/windows/EventToken.h"),
            )
        }
    }
    linuxX64Target.compilations["main"].cinterops.create("linuxGtk") {
        definitionFile.set(project.file("src/nativeInterop/linux/kauri_gtk.def"))
        compilerOpts("-I${project.file("src/nativeInterop/linux").absolutePath}")
        tasks.named(interopProcessingTaskName) {
            inputs.file(layout.projectDirectory.file("src/nativeInterop/linux/kauri_gtk.h"))
        }
    }
    linuxArm64Target.compilations["main"].cinterops.create("linuxGtk") {
        definitionFile.set(project.file("src/nativeInterop/linux/kauri_gtk.def"))
        compilerOpts("-I${project.file("src/nativeInterop/linux").absolutePath}")
        tasks.named(interopProcessingTaskName) {
            inputs.file(layout.projectDirectory.file("src/nativeInterop/linux/kauri_gtk.h"))
        }
    }
    macos.binaries.all {
        linkerOpts("-framework", "AppKit", "-framework", "WebKit")
    }
    windows.binaries.all {
        linkerOpts("-lole32", "-luser32")
    }
    linuxX64Target.binaries.all {
        linkerOpts("-L${rootProject.layout.buildDirectory.dir("native-deps/linuxX64").get().dir("lib").asFile.absolutePath}")
        linkTaskProvider.configure {
            inputs.dir(rootProject.layout.buildDirectory.dir("native-deps/linuxX64").get().dir("lib"))
        }
    }
    linuxArm64Target.binaries.all {
        linkerOpts("-L${rootProject.layout.buildDirectory.dir("native-deps/linuxArm64").get().dir("lib").asFile.absolutePath}")
        linkTaskProvider.configure {
            inputs.dir(rootProject.layout.buildDirectory.dir("native-deps/linuxArm64").get().dir("lib"))
        }
    }
    sourceSets {
        commonMain.dependencies { api(libs.kotlinx.io) }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
