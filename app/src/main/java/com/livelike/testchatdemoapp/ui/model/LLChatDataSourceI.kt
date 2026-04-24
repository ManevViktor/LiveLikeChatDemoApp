package com.livelike.testchatdemoapp.ui.model

import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow





typealias LLMessageList = List<LiveLikeChatMessage>


class LLChatDataSourceI(val chatDataClient : ILiveLikeDataSource<LLMessageList>) :
	ILiveLikeDataSource<LLMessageList> {
	
	
	override val chatMessagesFlow : StateFlow<List<LiveLikeChatMessage>> get() = chatDataClient.chatMessagesFlow
	override val backendResponseFlow : SharedFlow<LLChatBackendResponse<String>> get() = chatDataClient.backendResponseFlow
	override val loadNextMessagesFlow : SharedFlow<LLMessageList> get() = chatDataClient.loadNextMessagesFlow
	
	
	override fun loadMessages() {
		chatDataClient.loadMessages()
	}
	
	override fun loadNextMessages() {
		chatDataClient.loadNextMessages()
	}
	
	override fun connectToChatRoom(roomId : String) {
		chatDataClient.connectToChatRoom(roomId)
	}
	
	override fun blockProfile(id : String) {
		chatDataClient.blockProfile(id)
	}
	
	override fun sendMessage(message : String) {
		chatDataClient.sendMessage(message)
	}

	override fun resetChatSession() {
		chatDataClient.resetChatSession()
	}
	
	override fun close() {
		chatDataClient.close()
	}
	
	
	
}


interface ILiveLikeDataSource<T> {
	
	
	fun loadMessages()
	
	fun loadNextMessages()
	
	fun connectToChatRoom(roomId : String)
	
	fun blockProfile(id : String)
	
	fun sendMessage(message : String)

	fun resetChatSession()
	
	fun close()
	
	val chatMessagesFlow : StateFlow<T>

	val backendResponseFlow : SharedFlow<LLChatBackendResponse<String>>

	val loadNextMessagesFlow : SharedFlow<LLMessageList>

}


data class LiveLikeChatParams(
	val clientID : String,
	val token : String,
	val chatRoomID : String
)

sealed class LLChatBackendResponse<out T> {
	data class Success<T>(val data : T) : LLChatBackendResponse<T>()
	data class Error(val error : String) : LLChatBackendResponse<Nothing>()
}
