package com.guardiantrack.core.network.api

import com.guardiantrack.core.network.dto.SyncBatchRequest
import com.guardiantrack.core.network.dto.SyncBatchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GuardianSyncApi {
    @POST("v1/sync/batch")
    suspend fun uploadBatch(
        @Body request: SyncBatchRequest
    ): Response<SyncBatchResponse>
}
