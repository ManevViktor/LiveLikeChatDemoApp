package com.livelike.testchatdemoapp.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.livelike.testchatdemoapp.BuildConfig
import com.livelike.testchatdemoapp.data.AuthTokenStore
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class LLHistoryViewModel(
    private val historyDataSource: ILiveLikeDataSource<LLMessageList>
) : ViewModel() {

    val chatMessagesFlow: StateFlow<LLMessageList> = historyDataSource.chatMessagesFlow
    val backendResponseFlow: SharedFlow<LLChatBackendResponse<String>> = historyDataSource.backendResponseFlow
    val loadNextMessagesFlow: StateFlow<LLMessageList> = historyDataSource.loadNextMessagesFlow

    fun connectToChatRoom(roomId: String) {
        historyDataSource.connectToChatRoom(roomId)
    }

    fun loadNextMessages() {
        historyDataSource.loadNextMessages()
    }

    fun blockProfile(profileId: String) {
        historyDataSource.blockProfile(profileId)
    }

    fun closeChatSession() {
        historyDataSource.resetChatSession()
    }

    override fun onCleared() {
        historyDataSource.close()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (!modelClass.isAssignableFrom(LLHistoryViewModel::class.java)) {
                        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                    }

                    val tokenStore = AuthTokenStore(appContext)
                    val liveLikeClientId = BuildConfig.LIVELIKE_CLIENT_ID
                    require(liveLikeClientId.isNotBlank()) {
                        "Missing LIVELIKE_CLIENT_ID. Set livelike.clientId in local.properties or LIVELIKE_CLIENT_ID as a Gradle property."
                    }

                    val dataSource = LLChatDataSourceI(
                        LiveHistoryDataClient(
                            clientID = liveLikeClientId,
                            tokenStore = tokenStore
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    return LLHistoryViewModel(
                        historyDataSource = dataSource
                    ) as T
                }
            }
        }
    }
}
