pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        exclusiveContent {
            forRepository {
                ivy {
                    name = "BinaryenDistributions"
                    url = uri("https://github.com/WebAssembly/binaryen/releases/download")
                    patternLayout { artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}

rootProject.name = "Kauri"
include(":framework", ":app:shared", ":app:desktop", ":app:frontend")
