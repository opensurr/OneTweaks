package com.oneuitweak.app

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/** Runs shell commands through Shizuku (shell uid, no root). */
object ShizukuShell {

    data class Result(val ok: Boolean, val output: String)

    const val PERMISSION_REQUEST_CODE = 1001

    private val newProcessMethod by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }
    }

    val isReady: Boolean
        get() = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    val hasPermission: Boolean
        get() = runCatching {
            isReady && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    /**
     * Call this from the "Connect" button.
     *
     * - If Shizuku isn't running: returns false (caller should open Shizuku app to start the service).
     * - If running but permission not granted: triggers the Shizuku permission dialog and returns false.
     * - If already granted: returns true.
     */
    fun requestPermission(): Boolean {
        if (!isReady) return false
        if (hasPermission) return true
        runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
        return false
    }

    fun exec(vararg commands: String): Result {
        if (!isReady) return Result(false, "Shizuku is not running")
        if (!hasPermission) return Result(false, "Shizuku permission not granted")
        return try {
            val script = commands.joinToString(" && ")
            val process = newProcessMethod.invoke(
                null, arrayOf("sh", "-c", script), null, null
            ) as Process
            val out = process.inputStream.bufferedReader().use { it.readText() }
            val err = process.errorStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            Result(code == 0, (out + err).trim())
        } catch (t: Throwable) {
            // Unwrap InvocationTargetException so we show the real cause
            val cause = (t as? java.lang.reflect.InvocationTargetException)?.cause ?: t
            Result(false, cause.message ?: cause.javaClass.simpleName)
        }
    }

    fun query(namespace: String, key: String): String? {
        val r = exec("settings get $namespace $key")
        return if (r.ok && r.output.isNotBlank() && r.output != "null") r.output.trim() else null
    }
}
