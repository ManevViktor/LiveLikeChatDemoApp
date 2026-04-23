package com.livelike.testchatdemoapp.ui.model

import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import kotlinx.coroutines.flow.SharedFlow


const val clientID : String = "xyz"
var activeUserToken : String? = null


typealias LLMessageList = List<LiveLikeChatMessage>


class LLChatDataSourceI(val chatDataClient : ILiveLikeDataSource<LLMessageList> = LiveLikeDataClient(clientID, activeUserToken)) :
	ILiveLikeDataSource<LLMessageList> {
	
	
	override val chatMessagesFlow : SharedFlow<List<LiveLikeChatMessage>> get() = chatDataClient.chatMessagesFlow
	override val backendResponseFlow : SharedFlow<LLChatBackendResponse<String>> get() = chatDataClient.backendResponseFlow
	
	
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
	
	
	
}


interface ILiveLikeDataSource<T> {
	
	
	fun loadMessages()
	
	fun loadNextMessages()
	
	fun connectToChatRoom(roomId : String)
	
	fun blockProfile(id : String)
	
	fun sendMessage(message : String)
	
	val chatMessagesFlow : SharedFlow<T>
	
	val backendResponseFlow : SharedFlow<LLChatBackendResponse<String>>
	
}


data class LiveLikeChatParams(
	val clientID : String,
	val token : String,
	val chatRoomID : String
)

sealed class LLLoadChatMessageEvents {


}


sealed class LLChatBackendResponse<out T> {
	data class Success<T>(val data : T) : LLChatBackendResponse<T>()
	data class Error(val error : String) : LLChatBackendResponse<Nothing>()
}