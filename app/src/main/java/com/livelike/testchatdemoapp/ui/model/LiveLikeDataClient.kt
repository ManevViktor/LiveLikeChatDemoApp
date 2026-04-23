package com.livelike.testchatdemoapp.ui.model

import android.util.Log
import com.livelike.common.AccessTokenDelegate
import com.livelike.common.LiveLikeKotlin
import com.livelike.common.profile
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class LiveLikeDataClient(val clientID : String, val token : String?) : ILiveLikeDataSource<LLMessageList> {
	
	var sdkCLient : LiveLikeKotlin
	
	val messageFlow : MutableSharedFlow<LLMessageList> = MutableSharedFlow(1)
	val backendResponse : MutableSharedFlow<LLChatBackendResponse<String>> = MutableSharedFlow(1)
	
	var chatSession : LiveLikeChatSession? = null
	
	
	init {
		sdkCLient = createLiveLikeKotlinInstance(clientID, token)
	}
	
	
	private fun createLiveLikeKotlinInstance(clientID : String, token : String?) : LiveLikeKotlin {
		return LiveLikeKotlin(
			clientID,
			accessTokenDelegate = object : AccessTokenDelegate {
				override fun getAccessToken() : String? = token
				
				override fun storeAccessToken(accessToken : String?) = Unit
			}
		)
	}
	
	override val chatMessagesFlow : SharedFlow<LLMessageList>
		get() = messageFlow
	
	
	override val backendResponseFlow : SharedFlow<LLChatBackendResponse<String>>
		get() = backendResponse
	
	override fun loadMessages() {
		chatSession?.getLoadedMessages()
	}
	
	override fun loadNextMessages() {
		chatSession?.loadNextHistory { response, error ->
		
		}
	}
	
	override fun connectToChatRoom(roomId : String) {
		chatSession?.connectToChatRoom(roomId){result, error ->
			processBackendResponse("connect to chat room", result)
		}
	}
	
	override fun blockProfile(id : String) {
		sdkCLient.profile().blockProfile(profileId = id) { result, error ->
			processBackendResponse( "block profile" , result )
		}
	}
	
	override fun sendMessage(message : String) {
		chatSession?.sendMessage(
			message = message,
			liveLikePreCallback = { _, _ -> },
			callback = { result, error ->
				processBackendResponse( "send message" , result )
			})
	}
	

	
	private fun <R> processBackendResponse(action : String, result : R?) {
		if (result != null) {
			backendResponse.tryEmit(LLChatBackendResponse.Success("success $action"))
			Log.d( "ll backend action $action" , "success")
		} else {
			backendResponse.tryEmit(LLChatBackendResponse.Error("error $action"))
			Log.d( "ll backend action $action" , "error")
		}
	}
	
	
	
	
	
	
	

//Method to send message blocking
//	private suspend fun LiveLikeChatSession.sendMessageSuspended(
//		message : String
//	) : LiveLikeChatMessage {
//		return suspendCoroutine { continuation ->
//			sendMessage(
//				message = message,
//				liveLikePreCallback = { _, _ -> },
//				callback = { result, error ->
//					when {
//						result != null -> continuation.resume(result)
//						error != null -> continuation.resumeWithException(Throwable(error))
//						else -> continuation.resumeWithException(Throwable("Unable to send chat message"))
//					}
//				}
//			)
//		}
//	}
	
	
}

