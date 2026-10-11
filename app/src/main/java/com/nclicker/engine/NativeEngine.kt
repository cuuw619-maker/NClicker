package com.nclicker.engine

import kotlin.random.Random

/** Rust/JNI ticket generator with a Kotlin fallback for native-less IDE runs. */
object NativeEngine {
    private val nativeLoaded: Boolean = try {
        System.loadLibrary("plate_engine")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    } catch (_: SecurityException) {
        false
    }

    @JvmStatic
    private external fun nativeTicket(seed: Long, bound: Int): Int

    fun nextTicket(bound: Int): Int {
        if (bound <= 0) return 0
        val value = if (nativeLoaded) {
            runCatching { nativeTicket(System.nanoTime(), bound) }
                .getOrElse { Random.nextInt(bound) }
        } else {
            Random.nextInt(bound)
        }
        return value.coerceIn(0, bound - 1)
    }
}
