package com.wire.kalium.logic.data.conversation

import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.PlainId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserId

data class Conversation(
    val id: ConversationId,
    val name: String?,
    val type: Type,
    val teamId: TeamId?,
    val mutedStatus: MutedConversationStatus,
    val lastNotificationDate: String?,
    val lastModifiedDate: String?
) {
    enum class Type { SELF, ONE_ON_ONE, GROUP, CONNECTION_PENDING }
}

sealed class ConversationDetails(open val conversation: Conversation) {

    data class Self(override val conversation: Conversation) : ConversationDetails(conversation)

    data class OneOne(
        override val conversation: Conversation,
        val otherUser: OtherUser,
        val connectionState: ConnectionState,
        val legalHoldStatus: LegalHoldStatus,
        val userType: UserType
    ) : ConversationDetails(conversation)

    data class Group(
        override val conversation: Conversation,
        val legalHoldStatus: LegalHoldStatus,
        val hasOngoingCall: Boolean = false
    ) : ConversationDetails(conversation)

    data class Connection(
        val conversationId: ConversationId,
        val otherUser: OtherUser?,
        val userType: UserType,
        val lastModifiedDate: String?,
        val connection: com.wire.kalium.logic.data.user.Connection,
    ) : ConversationDetails(
        Conversation(
            id = conversationId,
            name = otherUser?.name,
            type = Conversation.Type.CONNECTION_PENDING,
            teamId = otherUser?.team?.let { TeamId(it) },
            mutedStatus = MutedConversationStatus.AllAllowed,
            lastNotificationDate = null,
            lastModifiedDate = lastModifiedDate,
        )
    )
}

class MembersInfo(val self: Member, val otherMembers: List<Member>)

class Member(override val id: UserId) : User()

sealed class MemberDetails {
    data class Self(val selfUser: SelfUser) : MemberDetails()
    data class Other(val otherUser: OtherUser, val userType: UserType) : MemberDetails()
}

typealias ClientId = PlainId

data class Recipient(val member: Member, val clients: List<ClientId>)

enum class UserType {
    INTERNAL,

    // TODO(user-metadata): for now External will not be implemented
    /**Team member with limited permissions */
    EXTERNAL,

    /**
     * A user on the same backend but not on your team or,
     * Any user on another backend using the Wire application,
     */
    FEDERATED,

    /**
     * Any user in wire.com using the Wire application or,
     * A temporary user that joined using the guest web interface,
     * from inside the backend network or,
     * A temporary user that joined using the guest web interface,
     * from outside the backend network
     */
    GUEST;
}
