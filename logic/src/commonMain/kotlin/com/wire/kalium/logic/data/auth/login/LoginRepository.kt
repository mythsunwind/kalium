package com.wire.kalium.logic.data.auth.login

import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.session.SessionMapper
import com.wire.kalium.logic.di.MapperProvider
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.wrapApiRequest
import com.wire.kalium.network.api.user.login.LoginApi

interface LoginRepository {
    suspend fun loginWithEmail(
        email: String, password: String, shouldPersistClient: Boolean
    ): Either<NetworkFailure, AuthSession.Tokens>

    suspend fun loginWithHandle(
        handle: String, password: String, shouldPersistClient: Boolean
    ): Either<NetworkFailure, AuthSession.Tokens>
}

class LoginRepositoryImpl(
    private val loginApi: LoginApi,
    private val clientLabel: String,
    private val sessionMapper: SessionMapper = MapperProvider.sessionMapper()
) : LoginRepository {

    override suspend fun loginWithEmail(
        email: String, password: String, shouldPersistClient: Boolean
    ): Either<NetworkFailure, AuthSession.Tokens> =
        login(LoginApi.LoginParam.LoginWithEmail(email, password, clientLabel), shouldPersistClient)

    override suspend fun loginWithHandle(
        handle: String, password: String, shouldPersistClient: Boolean
    ): Either<NetworkFailure, AuthSession.Tokens> =
        login(LoginApi.LoginParam.LoginWithHandel(handle, password, clientLabel), shouldPersistClient)

    private suspend fun login(
        loginParam: LoginApi.LoginParam, persistClient: Boolean
    ): Either<NetworkFailure, AuthSession.Tokens> = wrapApiRequest {
        loginApi.login(param = loginParam, persist = persistClient)
    }.map { sessionMapper.fromSessionDTO(it) }
}
