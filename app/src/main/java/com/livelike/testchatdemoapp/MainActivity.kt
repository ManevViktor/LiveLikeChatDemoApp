package com.livelike.testchatdemoapp

import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.livelike.testchatdemoapp.ui.chat.ChatScreen
import com.livelike.testchatdemoapp.ui.history.HistoryChatScreen
import com.livelike.testchatdemoapp.ui.home.MainMenuScreen
import com.livelike.testchatdemoapp.ui.model.LLHistoryViewModel
import com.livelike.testchatdemoapp.ui.model.LLChatViewModel
import com.livelike.testchatdemoapp.ui.theme.TestChatDemoAppTheme

private const val CHAT_ROOM_ID = "d8a6508f-3132-40d8-af08-653f450026b6"

private enum class MainDestination {
	MainMenu,
	LoadedChat,
	HistoryChat
}

class MainActivity : ComponentActivity() {
	private val chatViewModel: LLChatViewModel by viewModels {
		LLChatViewModel.factory(applicationContext)
	}
	private val historyViewModel: LLHistoryViewModel by viewModels {
		LLHistoryViewModel.factory(applicationContext)
	}

	private fun openChatScreen(onOpen: () -> Unit) {
		chatViewModel.closeChatSession()
		onOpen()
	}

	private fun openHistoryScreen(onOpen: () -> Unit) {
		historyViewModel.closeChatSession()
		onOpen()
	}

	private fun closeCurrentScreen(destination: MainDestination, onClose: () -> Unit) {
		when (destination) {
			MainDestination.LoadedChat -> chatViewModel.closeChatSession()
			MainDestination.HistoryChat -> historyViewModel.closeChatSession()
			MainDestination.MainMenu -> Unit
		}
		onClose()
	}

	private fun closeChatScreen(onClose: () -> Unit) {
		chatViewModel.closeChatSession()
		onClose()
	}

	override fun onCreate(savedInstanceState : Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			TestChatDemoAppTheme {
				var destination by rememberSaveable { mutableStateOf(MainDestination.MainMenu) }

				if (destination != MainDestination.MainMenu) {
					BackHandler {
						closeCurrentScreen(destination) {
							destination = MainDestination.MainMenu
						}
					}
					when (destination) {
						MainDestination.LoadedChat -> ChatScreen(
							viewModel = chatViewModel,
							roomId = CHAT_ROOM_ID,
							modifier = Modifier.fillMaxSize()
						)
						MainDestination.HistoryChat -> HistoryChatScreen(
							viewModel = historyViewModel,
							roomId = CHAT_ROOM_ID,
							modifier = Modifier.fillMaxSize()
						)
						MainDestination.MainMenu -> Unit
					}
				} else {
					MainMenuScreen(
						onChatLoadedMessagesClick = {
							openChatScreen {
								destination = MainDestination.LoadedChat
							}
						},
						onChatHistoryOnlyClick = {
							openHistoryScreen {
								destination = MainDestination.HistoryChat
							}
						},
						modifier = Modifier.fillMaxSize()
					)
				}
			}
		}
	}
}
