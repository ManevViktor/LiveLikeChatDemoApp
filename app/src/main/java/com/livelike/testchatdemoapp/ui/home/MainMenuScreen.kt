package com.livelike.testchatdemoapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.livelike.testchatdemoapp.ui.theme.TestChatDemoAppTheme

@Composable
fun MainMenuScreen(
	onChatLoadedMessagesClick : () -> Unit,
	modifier : Modifier = Modifier
) {
	Box(
		modifier = modifier
			.background(MaterialTheme.colorScheme.surface)
			.windowInsetsPadding(WindowInsets.safeDrawing)
			.padding(horizontal = 24.dp, vertical = 32.dp),
		contentAlignment = Alignment.Center
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = "Main Screen",
				style = MaterialTheme.typography.headlineMedium,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 8.dp)
			)
			Button(
				onClick = onChatLoadedMessagesClick,
				modifier = Modifier.fillMaxWidth()
			) {
				Text("Chat Loaded Messages")
			}
			OutlinedButton(
				onClick = {},
				enabled = false,
				modifier = Modifier.fillMaxWidth()
			) {
				Text("Option 2")
			}
			OutlinedButton(
				onClick = {},
				enabled = false,
				modifier = Modifier.fillMaxWidth()
			) {
				Text("Option 3")
			}
			OutlinedButton(
				onClick = {},
				enabled = false,
				modifier = Modifier.fillMaxWidth()
			) {
				Text("Option 4")
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun MainMenuScreenPreview() {
	TestChatDemoAppTheme {
		MainMenuScreen(
			onChatLoadedMessagesClick = {},
			modifier = Modifier.fillMaxSize()
		)
	}
}
