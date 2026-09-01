package com.frange.kmpacestream.shared.data.repository

import com.frange.kmpacestream.shared.data.remote.AceStreamApiClient
import com.frange.kmpacestream.shared.domain.core.NetworkError
import com.frange.kmpacestream.shared.domain.core.Result
import com.frange.kmpacestream.shared.domain.model.StreamInfo
import com.frange.kmpacestream.shared.domain.repository.AceStreamRepository
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.TimeoutCancellationException
import java.net.ConnectException

class AceStreamRepositoryImpl(
    private val apiClient: AceStreamApiClient
) : AceStreamRepository {


//    acestream://118416583faa3fb4f03800173429b6b844b43a52


    override suspend fun getStreamUrl(contentId: String): Result<StreamInfo, NetworkError> {
        val cleanId = contentId
            .trim()
            .replace("acestream://", "")
            .replace("http://", "")
            .replace("https://", "")

        if (cleanId.isEmpty()) {
            return Result.Error(NetworkError.ServerError)
        }

        return try {
            val response = apiClient.getStream(cleanId)
            val playbackUrl = response.response?.playbackUrl ?: response.response?.statUrl

            if (playbackUrl != null) {
                Result.Success(
                    StreamInfo(
                        playbackUrl = playbackUrl,
                        statUrl = response.response?.statUrl
                    )
                )
            } else {
                println("AceStream Engine Error: ${response.error}")
                Result.Error(NetworkError.ServerError)
            }
        } catch (e: TimeoutCancellationException) {
            Result.Error(NetworkError.RequestTimeout)
        } catch (e: ConnectException) {
            Result.Error(NetworkError.RequestTimeout)
        } catch (e: UnresolvedAddressException) {
            Result.Error(NetworkError.NoInternet)
        } catch (e: Exception) {
            Result.Error(NetworkError.Unknown)
        }
    }
}