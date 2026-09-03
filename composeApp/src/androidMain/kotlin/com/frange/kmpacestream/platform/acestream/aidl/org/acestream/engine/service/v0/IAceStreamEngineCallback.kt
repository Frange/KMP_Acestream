package com.frange.kmpacestream.platform.acestream.aidl.org.acestream.engine.service.v0

/**
 * Ace Stream Engine callback interface
 */
interface IAceStreamEngineCallback {

    /**
     * Engine unpacking (this happens after installation and after engine was updated).
     */
    fun onUnpacking()

    /**
     * Engine starting.
     */
    fun onStarting()

    /**
     * Engine started successfully and listens @listenPort
     * or failed to start,  if @listenPort == -1.
     */
    fun onReady(listenPort: Int)

    /**
     * Engine stopped.
     */
    fun onStopped()

    /**
     * Deprecated method. It is not called anymore.
     */
    fun onWaitForNetworkConnection()

    /**
     * Media server event: playlist was updated.
     */
    fun onPlaylistUpdated()

    /**
     * Media server event: EPG was updated.
     */
    fun onEPGUpdated()

    /**
     * Client should restart player (stop playback and start again with the same playback URI).
     */
    fun onRestartPlayer()

    /**
     * Engine settings were updated.
     */
    fun onSettingsUpdated()

    /**
     * Auth level updated.
     */
    fun onAuthUpdated()
}