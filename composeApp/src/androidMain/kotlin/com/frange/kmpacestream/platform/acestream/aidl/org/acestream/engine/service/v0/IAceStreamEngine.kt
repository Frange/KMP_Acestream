package com.frange.kmpacestream.platform.acestream.aidl.org.acestream.engine.service.v0

import com.frange.kmpacestream.platform.acestream.aidl.org.acestream.engine.service.v0.IAceStreamEngineCallback
import com.frange.kmpacestream.platform.acestream.aidl.org.acestream.engine.service.v0.IStartEngineResponse

/**
 * Ace Stream Engine service interface
 */
interface IAceStreamEngine {

    /**
     * Register a client.
     */
    fun registerCallback(cb: IAceStreamEngineCallback)

    /**
     * Unregister a client.
     */
    fun unregisterCallback(cb: IAceStreamEngineCallback)

    /**
     * Start engine.
     */
    fun startEngine()

    /**
     * Internal method. Not for public usage.
     */
    fun registerCallbackExt(cb: IAceStreamEngineCallback, skipMobileNetworksCheck: Boolean)

    /**
     * Start engine and receive result via callback.
     * NOTE: currently callback will be called only when engine is already started at the moment
     * of calling this method. You should rely on IAceStreamEngineCallback.onReady() event.
     */
    fun startEngineWithCallback(callback: IStartEngineResponse)

    /**
     * Get Engine API port.
     * Returns 0 if engine is not started.
     * @see http://wiki.acestream.org/wiki/index.php/Engine_API
     */
    fun getEngineApiPort(): Int

    /**
     * Get HTTP API port.
     * Returns 0 if engine is not started.
     * @see http://wiki.acestream.org/wiki/index.php/Engine_HTTP_API
     */
    fun getHttpApiPort(): Int

    /**
     * Internal method. Not for public usage.
     */
    fun getAccessToken(): String?

    /**
     * Start AceCast server.
     */
    fun enableAceCastServer()
}