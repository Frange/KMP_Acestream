package com.frange.kmpacestream.data.repository

import com.frange.kmpacestream.platform.AceStreamAndroidController
import com.frange.kmpacestream.shared.domain.core.NetworkError
import com.frange.kmpacestream.shared.domain.core.Result
import com.frange.kmpacestream.shared.domain.model.StreamInfo
import com.frange.kmpacestream.shared.domain.repository.AceStreamRepository

class AndroidAceStreamRepository(
    private val controller: AceStreamAndroidController
) : AceStreamRepository {

    override suspend fun getStreamUrl(contentId: String): Result<StreamInfo, NetworkError> {
        val result = controller.startSession(contentId)

        return if (result.isSuccess) {
            val playbackUrl = result.getOrThrow()
            Result.Success(StreamInfo(playbackUrl = playbackUrl))
        } else {
            val errorMessage = result.exceptionOrNull()?.message ?: ""

            val networkError = when {
                errorMessage.contains("premium", ignoreCase = true) -> NetworkError.Premium
                errorMessage.contains("refused", ignoreCase = true) -> NetworkError.Refused
                else -> NetworkError.Unknown
            }

            Result.Error(networkError)
        }
    }
}