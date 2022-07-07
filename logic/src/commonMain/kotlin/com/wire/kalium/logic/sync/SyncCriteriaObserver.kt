package com.wire.kalium.logic.sync

import com.wire.kalium.logic.data.client.ClientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal interface SyncStartCriteriaResolution {
    object Ready : SyncStartCriteriaResolution
    class MissingRequirement(val cause: String) : SyncStartCriteriaResolution
}

internal interface SyncCriteriaObserver {
    suspend fun observeStartCriteria(): Flow<SyncStartCriteriaResolution>
}

internal class SyncCriteriaObserverImpl(private val clientRepository: ClientRepository) : SyncCriteriaObserver {
    override suspend fun observeStartCriteria(): Flow<SyncStartCriteriaResolution> = clientRepository.observeCurrentClientId().map {
        if (it == null) SyncStartCriteriaResolution.MissingRequirement("Missing client registration")
        else SyncStartCriteriaResolution.Ready
    }
}
