package com.wire.kalium.logic.data.message

import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.protobuf.messages.QualifiedConversationId

sealed class MessageContent {

    sealed class System : MessageContent()
    sealed class FromProto : MessageContent()
    sealed class Regular : FromProto()
    sealed class Signaling : FromProto()

    // client message content types
    data class Text(val value: String) : Regular()
    data class Calling(val value: String) : Regular()
    data class Asset(val value: AssetContent) : Regular()
    data class DeleteMessage(val messageId: String) : Regular()
    data class TextEdited(val editMessageId :String, val newContent : String) : Regular()
    data class DeleteForMe(
        val messageId: String,
        val conversationId: String,
        val qualifiedConversationId: QualifiedConversationId?
    ) : Regular()

    data class Unknown( // messages that aren't yet handled properly but stored in db in case
        val typeName: String? = null,
        val encodedData: ByteArray? = null,
        val hidden: Boolean = false
    ) : Regular()
    object Empty : Regular()

    // server message content types
    sealed class MemberChange(open val members: List<Member>) : System() {
        data class Added(override val members: List<Member>) : MemberChange(members)
        data class Removed(override val members: List<Member>) : MemberChange(members)
    }

    // we can add other types to be processed, but signaling ones shouldn't be persisted
    object Ignored : Signaling() // messages that aren't processed in any way
}
