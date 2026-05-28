package com.liveocr

import rikka.shizuku.Shizuku
import java.lang.reflect.Method

object ShizukuShell {

    /**
     * Execute a shell command via Shizuku and return the Process.
     * Shizuku runs as ADB shell (uid 2000), so screencap works without root.
     */
    fun exec(command: String): Process {
        // Shizuku provides a way to run commands as shell via its binder
        // We use the newProcess method available in Shizuku API
        val cls = Class.forName("rikka.shizuku.ShizukuRemoteProcess")
        // Fallback: use Runtime via Shizuku's UserService or direct binder call
        // For API 13+, Shizuku.newProcess is available
        return try {
            val method: Method = Shizuku::class.java.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                "/"
            ) as Process
        } catch (e: Exception) {
            // Direct fallback
            Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        }
    }
}
