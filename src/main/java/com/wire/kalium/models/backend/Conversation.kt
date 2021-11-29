//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//
package com.wire.kalium.models.backend

import com.wire.kalium.tools.UUIDSerializer
import java.util.*
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
        @Serializable(with = UUIDSerializer::class) val id: UUID,
        val name: String? = null,
        val members: List<ConversationMember>? = null  // todo fix me
) {
    @Serializable(with = UUIDSerializer::class)
    var creator: UUID? = null

    constructor(id: UUID, name: String?, members: List<ConversationMember>?, creator: UUID?) : this(
            id = id, name = name, members = members
    ) {
        this.creator = creator
    }
}
/*
        @Serializable(with = UUIDSerializer::class) val id: UUID,
        val name: String,
        @Serializable(with = UUIDSerializer::class) val creator: UUID,
        val members: List<ConversationMember>
)
*/
