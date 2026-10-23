package dev.kauri.host

import dev.kauri.api.SystemInfo

expect fun platformSystemInfo(): SystemInfo
expect fun platformUptime(): Long
expect fun platformReadText(path: String): String?
expect fun platformWriteText(directory: String, path: String, text: String)
