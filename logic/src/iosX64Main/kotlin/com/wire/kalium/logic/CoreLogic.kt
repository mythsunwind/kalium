package com.wire.kalium.logic

import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.auth.AuthenticationScope

actual class CoreLogic : CoreLogicCommon() {
    override fun getAuthenticationScope(): AuthenticationScope {
        TODO("Not yet implemented")
    }

    override fun getSessionScope(session: AuthSession): UserSessionScope {
        TODO("Not yet implemented")
    }
}
