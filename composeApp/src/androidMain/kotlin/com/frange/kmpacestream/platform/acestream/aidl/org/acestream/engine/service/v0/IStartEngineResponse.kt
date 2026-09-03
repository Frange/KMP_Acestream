package com.frange.kmpacestream.platform.acestream.aidl.org.acestream.engine.service.v0

/**
 * Callback to receive result of "startEngineWithCallback" command.
 */
interface IStartEngineResponse {
    fun onResult(success: Boolean)
}