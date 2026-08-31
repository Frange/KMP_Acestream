package com.frange.kmpacestream.shared.domain.repository

import com.frange.kmpacestream.shared.domain.core.NetworkError
import com.frange.kmpacestream.shared.domain.core.Result
import com.frange.kmpacestream.shared.domain.model.StreamInfo

interface AceStreamRepository {
    suspend fun getStreamUrl(contentId: String): Result<StreamInfo, NetworkError>
}