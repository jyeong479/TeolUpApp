package com.example.teolupapp.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.example.teolupapp.R
import com.example.teolupapp.models.Refri
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChatFragment : Fragment() {

    private val apiKey = "GEMINI_APIKEY"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    ChatScreen(apiKey)
                }
            }
        }
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChatScreen(apiKey: String) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var isLoading by remember { mutableStateOf(false) }

    // 리스트 스크롤 상태 제어를 위한 State
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-2.0-flash", // 안정적인 버전 사용 권장
            apiKey = apiKey
        )
    }

    val chat = remember {
        generativeModel.startChat(
            history = listOf(
                content(role = "user") { text("안녕하세요! 냉장고 재료로 레시피를 추천해주세요.") },
                content(role = "model") { text("네, 냉장고에 있는 재료를 알려주시면 맛있는 레시피를 추천해 드릴게요! 어떤 재료가 있나요?") }
            )
        )
    }

    // 메시지가 추가되거나 로딩 상태가 바뀌면 맨 아래로 스크롤
    LaunchedEffect(messages.size, isLoading) {
        val totalItems = messages.size + if (isLoading) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(ChatMessage("냉장고를 확인하고 있습니다...", false))
            isLoading = true

            try {
                val ingredients = getIngredientsFromFirebase()

                if (messages.isNotEmpty()) {
                    messages.removeAt(messages.size - 1)
                }

                if (ingredients.isBlank()) {
                    messages.add(ChatMessage("냉장고가 비어있네요! 재료를 먼저 등록해주세요.", false))
                    isLoading = false
                    return@LaunchedEffect
                }

                val prompt = "내 냉장고에 있는 재료들이야:\n$ingredients\n\n이 재료들을 활용해서 유통기한이 급한 순서대로 만들 수 있는 맛있는 레시피 2가지를 추천해줘. 필요한 재료와 간단한 조리법도 알려줘."

                messages.add(ChatMessage("냉장고 재료를 확인했습니다. 레시피를 생각 중입니다...", false))

                val response = chat.sendMessage(prompt)

                if (messages.isNotEmpty()) {
                    messages.removeAt(messages.size - 1)
                }
                response.text?.let { messages.add(ChatMessage(it, false)) }

            } catch (e: Exception) {
                if (messages.isNotEmpty()) {
                    messages.removeAt(messages.size - 1)
                }
                messages.add(ChatMessage("오류가 발생했습니다: ${e.localizedMessage}", false))
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState, // 스크롤 상태 연결
            reverseLayout = false
        ) {
            items(messages) { message ->
                MessageBubble(message)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (isLoading) {
                item {
                    Text("AI가 입력 중...", modifier = Modifier.padding(8.dp), color = Color.Gray)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val primaryColor = colorResource(id = R.color.primary)

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("추가 질문을 입력하세요") },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = primaryColor,
                    cursorColor = primaryColor,
                    focusedLabelColor = primaryColor
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (messageText.isNotBlank() && !isLoading) {
                        val userMsg = messageText
                        messages.add(ChatMessage(userMsg, true))
                        messageText = ""
                        isLoading = true

                        coroutineScope.launch {
                            try {
                                val response = chat.sendMessage(userMsg)
                                response.text?.let {
                                    messages.add(ChatMessage(it, false))
                                }
                            } catch (e: Exception) {
                                messages.add(ChatMessage("오류: ${e.localizedMessage}", false))
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                )
            ) {
                Text("전송")
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val align = if (message.isUser) Alignment.End else Alignment.Start

    val backgroundColor = if (message.isUser) colorResource(id = R.color.primary) else Color.White
    val contentColor = if (message.isUser) Color.White else Color.Black

    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (message.isUser) 60.dp else 0.dp,
                end = if (message.isUser) 0.dp else 60.dp
            ),
        horizontalAlignment = align
    ) {
        Surface(
            color = backgroundColor,
            shape = bubbleShape,
            shadowElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = contentColor,
                fontSize = 16.sp
            )
        }
    }
}

// Firebase에서 재료 가져오기 (Suspend 함수)
@RequiresApi(Build.VERSION_CODES.O)
suspend fun getIngredientsFromFirebase(): String = suspendCancellableCoroutine { cont ->
    val database = FirebaseDatabase.getInstance().getReference("refri")
    database.get().addOnSuccessListener { snapshot ->
        val sb = StringBuilder()
        var count = 1
        val today = LocalDate.now().toString()
        
        sb.append("오늘 날짜: $today\n")
        
        if (snapshot.exists()) {
            for (child in snapshot.children) {
                val refri = child.getValue(Refri::class.java)
                if (refri != null) {
                    sb.append("$count. ${refri.name} (유통기한: ${refri.expiryDate}, 분류: ${refri.category})\n")
                    count++
                }
            }
            cont.resume(sb.toString())
        } else {
            cont.resume("") // 데이터 없음
        }
    }.addOnFailureListener { exception ->
        cont.resumeWithException(exception)
    }
}