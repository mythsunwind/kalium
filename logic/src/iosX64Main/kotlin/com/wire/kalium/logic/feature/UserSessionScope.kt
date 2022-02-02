package com.wire.kalium.logic.feature

import com.wire.kalium.logic.AuthenticatedDataSourceSet
import com.wire.kalium.logic.configuration.ClientConfig
import com.wire.kalium.persistence.db.Database

actual class UserSessionScope(override val database: Database, override val clientConfig: ClientConfig,
                              authenticatedDataSourceSet: AuthenticatedDataSourceSet
) : UserSessionScopeCommon(authenticatedDataSourceSet)
