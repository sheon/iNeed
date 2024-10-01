package lend.borrow.tool.chatroom

import Message
import User
import android.app.Activity
import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import lend.borrow.tool.shared.R
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatroomScreen(conversationId: String, loggedInUser: User, toUserId: String) {
    val application = (LocalContext.current as Activity).application
    val chatroomViewModel:  ChatroomViewModel= viewModel {
        ChatroomViewModel (conversationId, toUserId, application)
    }
    ChatroomContent(chatroomViewModel, loggedInUser)
}

@Composable
fun ChatroomContent(
    chatroomViewModel: ChatroomViewModel,
    loggedInUser: User
){

    Column(Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween) {
        MessageListScreen(chatroomViewModel, loggedInUser, chatroomViewModel.toUser, Modifier
            .weight(1f)
            .fillMaxWidth())
        SendMessage(chatroomViewModel, loggedInUser)
    }
}

@Composable
fun MessageListScreen(chatroomViewModel: ChatroomViewModel, me: User, toUser: User, modifier: Modifier){
    val messages = chatroomViewModel.messageListState.sortedBy { it.timeStampInSecond }
    val scrollState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) scrollState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier) {
        LazyColumn (
            state = scrollState
        ) {
            items(messages,
                key = {
                    it.messageId
                }) { item ->
                ChatItemBubble(item, item.fromUserId == me.id, toUser)
            }
        }
    }
}

@Composable
fun SendMessage(chatroomViewModel: ChatroomViewModel, loggedInUser: User) {
    val backgroundColor = Color(LocalContext.current.resources.getColor(R.color.primary))
    var message by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = message,
        onValueChange = {
            message = it
        },
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(5.dp),
        trailingIcon = {
            IconButton(
                enabled = message.isNotEmpty(),
                onClick = {
                    chatroomViewModel.sendMessage(message, loggedInUser)
                    message = ""
                }
            ) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.Send, contentDescription = "search")
            }
        },
        placeholder = {
            Text(text = "Send a message")
        },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = backgroundColor,
            unfocusedIndicatorColor = backgroundColor.copy(alpha = 0.3f),
            unfocusedContainerColor = backgroundColor.copy(alpha = 0.2f),
            focusedContainerColor = backgroundColor.copy(alpha = 0.2f),
            unfocusedTextColor = backgroundColor,
            focusedTextColor = backgroundColor,
            focusedLabelColor = backgroundColor,
            unfocusedLabelColor = backgroundColor,
            unfocusedTrailingIconColor = backgroundColor,
            focusedTrailingIconColor = backgroundColor
        )
    )
}

@Composable
fun ChatItemBubble(
    message: Message,
    isUserMe: Boolean,
    otherUser: User
) {
    val backgroundColor = Color(LocalContext.current.resources.getColor(R.color.primary))
    val calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat(
        "(HH:mm:ss) dd MMM yyyy", Locale.getDefault()
    )

    calendar.timeInMillis = message.timeStampInSecond.toLong() * 1000
    Column(
        Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalAlignment = if(isUserMe) Alignment.End else Alignment.Start
    ) {
        Text(formatter.format(calendar.time), color = Color.LightGray, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Column(
            Modifier
                .defaultMinSize(minWidth = 150.dp)
                .padding(
                    start = if (isUserMe) 40.dp else 0.dp,
                    end = if (!isUserMe) 40.dp else 0.dp
                )
                .background(
                    color = if (isUserMe) backgroundColor.copy(alpha = 0.7f) else Color.LightGray,
                    shape = if (isUserMe) RightChatBubbleShape else LeftChatBubbleShape
                )
                .padding(10.dp)
        ) {
            Text("${if (isUserMe)"Me" else otherUser.name}:", Modifier.wrapContentWidth(), color = if (isUserMe) Color.Yellow else Color.Black)
            Spacer(Modifier.height(5.dp))
            Text(message.message, color = if (isUserMe) Color.White else Color.Black)
        }

    }
}

private val LeftChatBubbleShape = RoundedCornerShape(2.dp, 15.dp, 15.dp, 20.dp)
private val RightChatBubbleShape = RoundedCornerShape(15.dp, 2.dp, 20.dp, 15.dp)