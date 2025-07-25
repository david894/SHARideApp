@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kxxr.sharide.db.ChatBotViewModel
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kxxr.sharide.R
import com.kxxr.sharide.db.ChatbotMessage

@Composable
fun ChatbotScreen(viewModel: ChatBotViewModel = viewModel()) {
    var userInput by remember { mutableStateOf("") }
    val botResponse by viewModel.response.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val messages = remember { mutableStateListOf<ChatbotMessage>() }
    val chatbotHeader = stringResource(id = R.string.chatbot_header)
    val chatbotTyping = stringResource(id = R.string.chatbot_typing)
    val contentPlaceHolder = stringResource(id = R.string.content_place_holder_chatbot)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatbotHeader, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0075FD),
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                reverseLayout = true,
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            //Typing indicator
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 80.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.DarkGray, shape = MaterialTheme.shapes.medium)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(chatbotTyping, color = Color.White)
                        }
                    }
                }
            }

            // user input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text(contentPlaceHolder) },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (userInput.isNotBlank()) {
                            val userMessage = ChatbotMessage(userInput, isUser = true)
                            messages.add(userMessage)

                            viewModel.generateContent(userInput)
                            userInput = ""
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color(0xFF0075FD)
                    )
                }
            }

            // bot response after generation
            LaunchedEffect(botResponse) {
                if (botResponse.isNotBlank()) {
                    messages.add(ChatbotMessage(botResponse, isUser = false))
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatbotMessage) {
    val bubbleColor = if (message.isUser) Color(0xFF0075FD) else Color.DarkGray
    val textColor = Color.White

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(color = bubbleColor, shape = MaterialTheme.shapes.medium)
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
