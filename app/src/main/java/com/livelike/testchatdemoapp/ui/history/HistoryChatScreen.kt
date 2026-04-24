package com.livelike.testchatdemoapp.ui.history

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.testchatdemoapp.ui.model.LLChatBackendResponse
import com.livelike.testchatdemoapp.ui.model.LLHistoryViewModel
import com.livelike.testchatdemoapp.ui.theme.TestChatDemoAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun HistoryChatScreen(
    viewModel: LLHistoryViewModel,
    roomId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.chatMessagesFlow.collectAsState()
    val historyItems = remember(messages) {
        messages.mapIndexed { index, message ->
            message.toHistoryUi(index)
        }
    }
    var previousLastMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel, roomId) {
        viewModel.connectToChatRoom(roomId)
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        HistoryMessageList(
            messages = historyItems,
            listState = listState,
            onBlockUser = { senderId ->
                if (senderId.isNotBlank()) {
                    viewModel.blockProfile(senderId)
                }
            },
            modifier = Modifier.weight(1f)
        )
    }

    LaunchedEffect(listState, historyItems.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            totalItems > 0 && lastVisibleIndex >= totalItems - 1
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                viewModel.loadNextMessages()
            }
    }

    LaunchedEffect(historyItems.lastOrNull()?.id) {
        val lastMessageId = historyItems.lastOrNull()?.id ?: return@LaunchedEffect
        val shouldScrollToBottom = previousLastMessageId != null && previousLastMessageId != lastMessageId
        previousLastMessageId = lastMessageId
        if (shouldScrollToBottom) {
            listState.animateScrollToItem(historyItems.lastIndex)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.backendResponseFlow.collect { response ->
            if (response is LLChatBackendResponse.Success) {
                if (response.data.contains("connect to chat room")) {
                    viewModel.loadNextMessages()
                }
                if (response.data.contains("block profile")) {
                    Toast.makeText(context, response.data, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun HistoryMessageList(
    messages: List<HistoryMessageItemUi>,
    listState: LazyListState,
    onBlockUser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) {
        HistoryEmptyState(modifier = modifier)
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = messages,
            key = { index, item -> "${item.id}-${item.timestamp}-${index}" }
        ) { _, item ->
            HistoryMessageCard(
                item = item,
                onBlockUser = onBlockUser
            )
        }
    }
}

@Composable
private fun HistoryMessageCard(
    item: HistoryMessageItemUi,
    onBlockUser: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = {
                    if (!item.senderId.isNullOrBlank()) {
                        showMenu = true
                    }
                }
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Text(
                    text = item.chatMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Block user") },
                onClick = {
                    showMenu = false
                    item.senderId?.let(onBlockUser)
                }
            )
        }
    }
}

@Composable
private fun HistoryEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No history messages yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Connect to a room and load history pages to populate the list.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class HistoryMessageItemUi(
    val id: String,
    val nickname: String,
    val chatMessage: String,
    val timestamp: String,
    val senderId: String?
)

private fun LiveLikeChatMessage.toHistoryUi(index: Int): HistoryMessageItemUi {
    return HistoryMessageItemUi(
        id = id ?: "history-message-$index",
        nickname = nickname?.takeIf { it.isNotBlank() } ?: "Unknown sender",
        chatMessage = message?.takeIf { it.isNotBlank() } ?: "",
        timestamp = formatHistoryTimestamp(timeStamp),
        senderId = senderId
    )
}

private fun formatHistoryTimestamp(rawTimestamp: String?): String {
    val millis = rawTimestamp?.toLongOrNull() ?: return ""
    val normalizedMillis = if (millis < 100_000_000_000L) millis * 1000 else millis
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(normalizedMillis))
}

@Preview(showBackground = true)
@Composable
private fun HistoryMessageListPreview() {
    TestChatDemoAppTheme {
        HistoryMessageList(
            messages = listOf(
                HistoryMessageItemUi(
                    id = "1",
                    nickname = "Ava",
                    chatMessage = "Kickoff starts in five minutes.",
                    timestamp = "09:12",
                    senderId = "sender-1"
                ),
                HistoryMessageItemUi(
                    id = "2",
                    nickname = "Marcus",
                    chatMessage = "Camera three is live and audio checks are clean.",
                    timestamp = "09:14",
                    senderId = "sender-2"
                )
            ),
            listState = rememberLazyListState(),
            onBlockUser = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
