package lend.borrow.tool.conversations

import User
import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun ConversationsScreen(loggedInUser: User, navController: NavController){
    val application = (LocalContext.current as Activity).application
    val conversationsViewModel: ConversationsViewModel = viewModel(key = loggedInUser.id) {
        ConversationsViewModel(application, loggedInUser.id)
    }
    val conversations by conversationsViewModel.conversationsMap.collectAsState(emptyMap<String, ConversationUiState>())
    Box(Modifier
        .fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        when {
            conversations.keys.isEmpty() -> {
                Text("No ongoing conversations")
            }
            else ->
                LazyColumn {
                    items(conversations.toList(),
                        key = { item ->
                            item.first
                        }) { item ->
                        Text(item.first)
                    }
                }
        }
    }


}