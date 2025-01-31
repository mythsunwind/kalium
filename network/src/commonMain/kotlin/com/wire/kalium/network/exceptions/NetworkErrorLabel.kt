package com.wire.kalium.network.exceptions

internal object NetworkErrorLabel {
    const val TOO_MANY_CLIENTS = "too-many-clients"
    const val INVALID_CREDENTIALS = "invalid-credentials"
    const val INVALID_EMAIL = "invalid-email"
    const val BAD_REQUEST = "bad-request"
    const val MISSING_AUTH = "missing-auth"
    const val DOMAIN_BLOCKED_FOR_REGISTRATION = "domain-blocked-for-registration"
    const val KEY_EXISTS = "key-exists"
    const val BLACKLISTED_EMAIL = "blacklisted-email"
    const val INVALID_CODE = "invalid-code"
    const val USER_CREATION_RESTRICTED = "user-creation-restricted"
    const val TOO_MANY_MEMBERS = "too-many-team-members"
    const val INVALID_HANDLE = "invalid-handle"
    const val HANDLE_EXISTS = "handle-exists"
    const val NO_TEAM = "no-team"
    const val OPERATION_DENIED = "operation-denied"

    object KaliumCustom {
        const val MISSING_REFRESH_TOKEN = "missing-refresh_token"
    }
}
