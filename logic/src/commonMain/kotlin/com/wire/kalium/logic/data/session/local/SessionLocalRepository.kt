package com.wire.kalium.logic.data.session.local

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.session.SessionMapper
import com.wire.kalium.logic.failure.SessionFailure
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.persistence.client.SessionDAO
import com.wire.kalium.persistence.model.PreferencesResult

interface SessionLocalRepository {
    suspend fun storeSession(autSession: AuthSession)
    suspend fun getSessions(): Either<CoreFailure, List<AuthSession>>
}

class SessionLocalDataSource(
    private val sessionLocalDataSource: SessionDAO,
    private val sessionMapper: SessionMapper
) : SessionLocalRepository {
    override suspend fun storeSession(autSession: AuthSession) =
        sessionLocalDataSource.addSession(sessionMapper.toPersistenceSession(autSession))


    override suspend fun getSessions(): Either<CoreFailure, List<AuthSession>> =
        when (val result = sessionLocalDataSource.allSessions()) {
            is PreferencesResult.Success -> Either.Right(
                result.data.values.toList().map { sessionMapper.fromPersistenceSession(it) })

            is PreferencesResult.DataNotFound -> Either.Left(SessionFailure.NoSessionFound)
        }

}
