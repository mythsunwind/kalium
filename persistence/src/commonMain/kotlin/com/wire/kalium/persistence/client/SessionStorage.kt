package com.wire.kalium.persistence.client

import com.wire.kalium.persistence.dao.UserIDEntity
import com.wire.kalium.persistence.kaliumLogger
import com.wire.kalium.persistence.kmm_settings.KaliumPreferences
import com.wire.kalium.persistence.model.AuthSessionEntity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

interface SessionStorage {
    /**
     * store a session locally
     */
    fun addSession(authSessionEntity: AuthSessionEntity)

    /**
     * delete a session from the local storage
     */
    fun deleteSession(userId: UserIDEntity)

    /**
     * returns the current active user session
     */
    fun currentSession(): AuthSessionEntity?

    /**
     * returns the Flow of current active user session
     */
    fun currentSessionFlow(): Flow<AuthSessionEntity?>

    /**
     * changes the current active user session
     */
    fun setCurrentSession(userId: UserIDEntity)

    /**
     * return all stored session as a userId to session map
     */
    fun allSessions(): Map<UserIDEntity, AuthSessionEntity>?

    /**
     * return stored session associated with a userId
     */
    fun userSession(userId: UserIDEntity): AuthSessionEntity?

    /**
     * returns true if there is any session saved and false otherwise
     */
    fun sessionsExist(): Boolean
}

class SessionStorageImpl(
    private val kaliumPreferences: KaliumPreferences
) : SessionStorage {

    private val sessionsUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun addSession(authSessionEntity: AuthSessionEntity) =
        allSessions()?.let { sessionMap ->
            val temp = sessionMap.toMutableMap()
            temp[authSessionEntity.userId] = authSessionEntity
            saveAllSessions(SessionsMap(temp))
        } ?: run {
            val sessions = mapOf(authSessionEntity.userId to authSessionEntity)
            saveAllSessions(SessionsMap(sessions))
        }.also { sessionsUpdatedFlow.tryEmit(Unit) }

    override fun deleteSession(userId: UserIDEntity) =
        allSessions()?.let { sessionMap ->
            // save the new map if the remove did not return null (session was deleted)
            val temp = sessionMap.toMutableMap()
            temp.remove(userId)?.let {
                sessionsUpdatedFlow.tryEmit(Unit)
                if (temp.isEmpty()) {
                    // in case it was the last session then delete sessions key/value from the file
                    removeAllSession()
                } else {
                    saveAllSessions(SessionsMap(temp))
                }
            } ?: run {
                kaliumLogger.d("trying to delete user session that didn't exists, userId: $userId")
            }
        } ?: run {
            kaliumLogger.d("trying to delete user session but no sessions are stored userId: $userId")
        }


    override fun currentSession(): AuthSessionEntity? =
        kaliumPreferences.getSerializable(CURRENT_SESSION_KEY, UserIDEntity.serializer())?.let { userId ->
            allSessions()?.let { sessionMap ->
                sessionMap[userId]
            }
        }

    override fun currentSessionFlow(): Flow<AuthSessionEntity?> = sessionsUpdatedFlow
        .map { currentSession() }
        .onStart { emit(currentSession()) }
        .distinctUntilChanged()

    override fun setCurrentSession(userId: UserIDEntity) =
        kaliumPreferences.putSerializable(CURRENT_SESSION_KEY, userId, UserIDEntity.serializer())
            .also { sessionsUpdatedFlow.tryEmit(Unit) }

    override fun allSessions(): Map<UserIDEntity, AuthSessionEntity>? =
        kaliumPreferences.getSerializable(SESSIONS_KEY, SessionsMap.serializer())?.sessions?.ifEmpty { null }

    override fun userSession(userId: UserIDEntity): AuthSessionEntity? =
        allSessions()?.let { sessionMap ->
            sessionMap[userId]
        }

    override fun sessionsExist(): Boolean = kaliumPreferences.hasValue(SESSIONS_KEY)

    private fun saveAllSessions(sessions: SessionsMap) {
        kaliumPreferences.putSerializable(SESSIONS_KEY, sessions, SessionsMap.serializer())
    }

    private fun removeAllSession() = kaliumPreferences.remove(SESSIONS_KEY)


    private companion object {
        private const val SESSIONS_KEY = "session_data_store_key"
        private const val CURRENT_SESSION_KEY = "current_session_key"
    }
}

// No actual instantiation of class 'SessionsMap' happens
// At runtime an object of 'SessionsMap' contains just 'Map<String, PersistenceSession>'
@Serializable
@JvmInline
private value class SessionsMap(@SerialName("s") val sessions: Map<UserIDEntity, AuthSessionEntity>)
