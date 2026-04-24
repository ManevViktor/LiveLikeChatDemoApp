package com.livelike.testchatdemoapp.ui.model

import android.util.Log
import com.livelike.common.AccessTokenDelegate
import com.livelike.common.LiveLikeKotlin
import com.livelike.common.profile
import com.livelike.engagementsdk.MessageWithReactionListener
import com.livelike.engagementsdk.chat.ChatMessageReaction
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.chat.data.remote.PinMessageInfo
import com.livelike.engagementsdk.createChatSession
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.testchatdemoapp.data.AuthTokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class LiveHistoryDataClient(
    private val clientID: String,
    private val tokenStore: AuthTokenStore
) : ILiveLikeDataSource<LLMessageList> {

    private val sdkClient: LiveLikeKotlin = createLiveLikeKotlinInstance(clientID)
    private val historyMessagesFlow = MutableStateFlow<LLMessageList>(emptyList())
    private val backendResponse = MutableSharedFlow<LLChatBackendResponse<String>>(extraBufferCapacity = 1)
    private val loadNextMessages = MutableStateFlow<LLMessageList>(emptyList())

    private var chatSession: LiveLikeChatSession? = null

    override val chatMessagesFlow: StateFlow<LLMessageList>
        get() = loadNextMessages

    override val backendResponseFlow: SharedFlow<LLChatBackendResponse<String>>
        get() = backendResponse

    override val loadNextMessagesFlow: StateFlow<LLMessageList>
        get() = loadNextMessages

    override fun loadMessages() = Unit

    override fun loadNextMessages() {
        val session = chatSession
        if (session == null) {
            emitBackendError("load next messages", "Chat session is not connected")
            return
        }

        session.loadNextHistory(callbackWithSeverity = { result, error ->
            if (result != null) {
                loadNextMessages.value += result
                emitBackendSuccess("load next messages")
            } else {
                emitBackendError("load next messages", error?.errorMessage)
            }
        })
    }

    override fun connectToChatRoom(roomId: String) {
        val session = ensureChatSession()
        session.connectToChatRoom(roomId) { result, error ->
            if (result != null) {
                emitBackendSuccess("connect to chat room")
            } else {
                emitBackendError("connect to chat room", error?.errorMessage)
            }
        }
    }

    override fun blockProfile(id: String) {
        sdkClient.profile().blockProfile(profileId = id) { result, error ->
            if (result != null) {
                emitBackendSuccess("block profile")
            } else {
                emitBackendError("block profile", error)
            }
        }
    }

    override fun sendMessage(message: String) {
        emitBackendError("send message", "History screen is read-only")
    }

    override fun resetChatSession() {
        chatSession?.close()
        chatSession = null
        historyMessagesFlow.value = emptyList()
        loadNextMessages.value = emptyList()
    }

    override fun close() {
        resetChatSession()
        sdkClient.close()
    }

    private fun createLiveLikeKotlinInstance(clientID: String): LiveLikeKotlin {
        return LiveLikeKotlin(
            clientID,
            accessTokenDelegate = object : AccessTokenDelegate {
                override fun getAccessToken(): String? = tokenStore.getTokenBlocking()

                override fun storeAccessToken(accessToken: String?) {
                    tokenStore.saveTokenAsync(accessToken)
                }
            }
        )
    }

    private fun ensureChatSession(): LiveLikeChatSession {
        chatSession?.let { return it }

        val session = sdkClient.createChatSession()
        session.setMessageWithReactionListener(object : MessageWithReactionListener {
            override fun onNewMessage(message: LiveLikeChatMessage) = Unit

            override fun onHistoryMessage(messages: List<LiveLikeChatMessage>) {
                historyMessagesFlow.value = (historyMessagesFlow.value + messages).distinctBy { it.id }
            }

            override fun onDeleteMessage(messageId: String) = Unit

            override fun onPinMessage(message: PinMessageInfo) = Unit

            override fun onUnPinMessage(pinMessageId: String) = Unit

            override fun onErrorMessage(error: String, clientMessageId: String?) {
                emitBackendError("chat listener", error)
            }

            override fun addMessageReaction(
                messagePubnubToken: Long?,
                messageId: String?,
                chatMessageReaction: ChatMessageReaction
            ) = Unit

            override fun removeMessageReaction(
                messagePubnubToken: Long?,
                messageId: String?,
                emojiId: String,
                userId: String?
            ) = Unit
        })
        chatSession = session
        return session
    }

    private fun emitBackendSuccess(action: String) {
        val message = "LiveLike API call success: $action"
        backendResponse.tryEmit(LLChatBackendResponse.Success(message))
        Log.d("ll history action $action", message)
    }

    private fun emitBackendError(action: String, error: String?) {
        val message = error ?: "error $action"
        backendResponse.tryEmit(LLChatBackendResponse.Error(message))
        Log.d("ll history action $action", message)
    }
}
