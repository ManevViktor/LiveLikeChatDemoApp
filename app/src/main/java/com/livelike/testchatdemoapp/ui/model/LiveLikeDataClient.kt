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

class LiveLikeDataClient(
    private val clientID: String,
    private val tokenStore: AuthTokenStore
) : ILiveLikeDataSource<LLMessageList> {

    private val sdkClient: LiveLikeKotlin = createLiveLikeKotlinInstance(clientID)
    private val messageFlow = MutableStateFlow<LLMessageList>(emptyList())
    private val backendResponse = MutableSharedFlow<LLChatBackendResponse<String>>(extraBufferCapacity = 1)
    private val loadNextMessages = MutableSharedFlow<LLMessageList>(extraBufferCapacity = 1)

    private var chatSession: LiveLikeChatSession? = null

    override val chatMessagesFlow: StateFlow<LLMessageList>
        get() = messageFlow

    override val backendResponseFlow: SharedFlow<LLChatBackendResponse<String>>
        get() = backendResponse

    override val loadNextMessagesFlow: SharedFlow<LLMessageList>
        get() = loadNextMessages

    override fun loadMessages() {
        emitLoadedChatMessages()
    }

    override fun loadNextMessages() {
        val session = chatSession
        if (session == null) {
            emitBackendError("load next messages", "Chat session is not connected")
            return
        }

        session.loadNextHistory(callbackWithSeverity = { result, error ->
            if (result != null) {
                
                emitLoadedChatMessages()
                //loadNextMessages.tryEmit(session.getLoadedMessages().toList())
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
                emitLoadedChatMessages()
                emitBackendSuccess("connect to chat room")
            } else {
                emitBackendError("connect to chat room", error?.errorMessage)
            }
        }
    }

    override fun blockProfile(id: String) {
        sdkClient.profile().blockProfile(profileId = id) { result, error ->
            if (result != null) {
                emitLoadedChatMessages()
                emitBackendSuccess("block profile")
            } else {
                emitBackendError("block profile", error)
            }
        }
    }

    override fun sendMessage(message: String) {
        val session = chatSession
        if (session == null) {
            emitBackendError("send message", "Chat session is not connected")
            return
        }

        session.sendMessage(
            message = message,
            liveLikePreCallback = { _, _ -> },
            callback = { result, error ->
                if (result != null) {
                    emitLoadedChatMessages()
                    emitBackendSuccess("send message")
                } else {
                    emitBackendError("send message", error)
                }
            }
        )
    }

    override fun resetChatSession() {
        chatSession?.close()
        chatSession = null
        messageFlow.value = emptyList()
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
            override fun onNewMessage(message: LiveLikeChatMessage) {
                emitLoadedChatMessages()
            }

            override fun onHistoryMessage(messages: List<LiveLikeChatMessage>) {
                emitLoadedChatMessages()
            }

            override fun onDeleteMessage(messageId: String) {
                emitLoadedChatMessages()
            }

            override fun onPinMessage(message: PinMessageInfo) = Unit

            override fun onUnPinMessage(pinMessageId: String) = Unit

            override fun onErrorMessage(error: String, clientMessageId: String?) {
                emitBackendError("chat listener", error)
            }

            override fun addMessageReaction(
                messagePubnubToken: Long?,
                messageId: String?,
                chatMessageReaction: ChatMessageReaction
            ) {
                emitLoadedChatMessages()
            }

            override fun removeMessageReaction(
                messagePubnubToken: Long?,
                messageId: String?,
                emojiId: String,
                userId: String?
            ) {
                emitLoadedChatMessages()
            }
        })
        chatSession = session
        return session
    }

    private fun emitLoadedChatMessages() {
        messageFlow.value = chatSession?.getLoadedMessages()?.toList().orEmpty()
    }

    private fun emitBackendSuccess(action: String) {
        val message = "LiveLike API call success: $action"
        backendResponse.tryEmit(LLChatBackendResponse.Success(message))
        Log.d("ll backend action $action", message)
    }

    private fun emitBackendError(action: String, error: String?) {
        val message = error ?: "error $action"
        backendResponse.tryEmit(LLChatBackendResponse.Error(message))
        Log.d("ll backend action $action", message)
    }
}
