package com.livelike.testchatdemoapp.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.livelike.testchatdemoapp.BuildConfig
import com.livelike.testchatdemoapp.data.AuthTokenStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class LLChatViewModel(
    private val chatDataSource: ILiveLikeDataSource<LLMessageList>,
    private val tokenStore: AuthTokenStore
) : ViewModel() {

    val chatMessagesFlow: StateFlow<LLMessageList> = chatDataSource.chatMessagesFlow
    val backendResponseFlow: SharedFlow<LLChatBackendResponse<String>> = chatDataSource.backendResponseFlow
    val loadNextMessagesFlow: SharedFlow<LLMessageList> = chatDataSource.loadNextMessagesFlow

    fun connectToChatRoom(roomId: String) {
        chatDataSource.connectToChatRoom(roomId)
    }

    fun loadMessages() {
        chatDataSource.loadMessages()
    }

    fun loadNextMessages() {
        chatDataSource.loadNextMessages()
    }

    fun blockProfile(profileId: String) {
        chatDataSource.blockProfile(profileId)
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        chatDataSource.sendMessage(message)
    }

    fun saveAccessToken(token: String) {
        viewModelScope.launch {
            tokenStore.saveToken(token)
        }
    }

    fun clearAccessToken() {
        tokenStore.clearTokenAsync()
    }
    
    fun closeChatSession()
    {
        chatDataSource.resetChatSession()
    }

    override fun onCleared() {
        chatDataSource.close()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (!modelClass.isAssignableFrom(LLChatViewModel::class.java)) {
                        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                    }

                    val tokenStore = AuthTokenStore(appContext)
                    val liveLikeClientId = BuildConfig.LIVELIKE_CLIENT_ID
                    require(liveLikeClientId.isNotBlank()) {
                        "Missing LIVELIKE_CLIENT_ID. Set livelike.clientId in local.properties or LIVELIKE_CLIENT_ID as a Gradle property."
                    }
                    val dataSource = LLChatDataSourceI(
                        LiveLikeDataClient(
                            clientID = liveLikeClientId,
                            tokenStore = tokenStore
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    return LLChatViewModel(
                        chatDataSource = dataSource,
                        tokenStore = tokenStore
                    ) as T
                }
            }
        }
    }
}
