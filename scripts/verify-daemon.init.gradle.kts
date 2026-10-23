import org.gradle.api.GradleException
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

abstract class ObservedDaemonIdentity : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
        val pid = ProcessHandle.current().pid()
        val javaHome = java.io.File(System.getProperty("java.home")).canonicalPath
        println("KAURI_DAEMON_OBSERVED $pid")
        return "$pid\u0000$javaHome"
    }
}

val identity = gradle.providers.of(ObservedDaemonIdentity::class.java) {}.get()
val actualPid = identity.substringBefore('\u0000').toLong()
val actualHome = java.io.File(identity.substringAfter('\u0000'))
val expectedPid = gradle.providers.environmentVariable("KAURI_EXPECTED_DAEMON_PID").get().toLong()
val expectedHome = java.io.File(gradle.providers.environmentVariable("JAVA_HOME").get()).canonicalFile
if (actualPid != expectedPid || actualHome != expectedHome) {
    throw GradleException("Kauri refused daemon $actualPid ($actualHome); expected $expectedPid ($expectedHome)")
}
println("KAURI_DAEMON_VERIFIED $actualPid")
